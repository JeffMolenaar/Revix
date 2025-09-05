#!/bin/bash

# Revix Database Import Script
# This script imports database data from an SQL file for restore purposes
# Can be used via SSH connection for remote database management

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Global variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Docker Compose command detection
DOCKER_COMPOSE_CMD=""

show_usage() {
    echo "Usage: $0 [OPTIONS] <SQL_FILE>"
    echo ""
    echo "Import SQL file to Revix database"
    echo ""
    echo "Options:"
    echo "  -c, --clean             Drop existing database before import (DESTRUCTIVE)"
    echo "  -n, --no-owner          Skip ownership and privilege statements"
    echo "  -v, --verbose           Verbose output"
    echo "  --docker-compose DIR    Docker compose directory (default: auto-detect)"
    echo "  --direct                Use direct PostgreSQL connection (requires credentials)"
    echo "  --confirm               Skip confirmation prompt (use with caution)"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Environment variables for direct connection:"
    echo "  REVIX_DB_HOST          Database host (default: localhost)"
    echo "  REVIX_DB_PORT          Database port (default: 5432)"
    echo "  REVIX_DB_NAME          Database name (default: revix)"
    echo "  REVIX_DB_USER          Database user (default: revix)"
    echo "  REVIX_DB_PASS          Database password (required)"
    echo ""
    echo "Examples:"
    echo "  $0 backup.sql                        # Import SQL file"
    echo "  $0 --clean backup.sql                # Clean import (drops existing data)"
    echo "  $0 --direct backup.sql               # Use direct PostgreSQL connection"
    echo "  $0 backup.sql.gz                     # Import compressed file"
    echo ""
    echo "IMPORTANT:"
    echo "  - Always backup your database before importing"
    echo "  - Use --clean option carefully as it will delete all existing data"
    echo "  - Ensure the SQL file is from a compatible Revix version"
}

detect_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null 2>&1; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        log_error "Docker Compose not found. Use --direct for direct PostgreSQL connection."
        return 1
    fi
    log_info "Using Docker Compose command: $DOCKER_COMPOSE_CMD"
}

check_docker_environment() {
    local compose_dir="${1:-$PROJECT_ROOT/deploy/docker}"
    
    if [ ! -f "$compose_dir/docker-compose.yml" ]; then
        log_error "Docker Compose file not found at $compose_dir/docker-compose.yml"
        return 1
    fi
    
    cd "$compose_dir"
    
    # Check if database container is running
    if ! $DOCKER_COMPOSE_CMD ps db | grep -q "Up"; then
        log_error "Database container is not running. Start it with: $DOCKER_COMPOSE_CMD up -d db"
        return 1
    fi
    
    # Test database connectivity
    if ! $DOCKER_COMPOSE_CMD exec -T db pg_isready -U revix -d revix > /dev/null 2>&1; then
        log_error "Database is not ready. Check database health."
        return 1
    fi
    
    log_success "Docker environment is ready"
    return 0
}

validate_sql_file() {
    local sql_file="$1"
    
    if [ ! -f "$sql_file" ]; then
        log_error "SQL file not found: $sql_file"
        return 1
    fi
    
    # Check if file is compressed
    if [[ "$sql_file" == *.gz ]]; then
        # Test if it's a valid gzip file
        if ! gzip -t "$sql_file" 2>/dev/null; then
            log_error "Invalid gzip file: $sql_file"
            return 1
        fi
        log_info "Detected compressed SQL file"
    else
        # Basic SQL file validation
        if ! head -n 5 "$sql_file" | grep -qE "(CREATE|INSERT|UPDATE|DELETE|DROP|ALTER)" 2>/dev/null; then
            log_warning "File does not appear to contain SQL statements"
            read -p "Continue anyway? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                return 1
            fi
        fi
    fi
    
    local file_size=$(du -h "$sql_file" | cut -f1)
    log_info "SQL file size: $file_size"
    return 0
}

create_backup_before_import() {
    local compose_dir="$1"
    local use_direct="$2"
    
    log_info "Creating backup before import..."
    
    local backup_dir="$PROJECT_ROOT/backups"
    mkdir -p "$backup_dir"
    
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_file="$backup_dir/pre_import_backup_$timestamp.sql"
    
    if [ "$use_direct" = "true" ]; then
        local db_host="${REVIX_DB_HOST:-localhost}"
        local db_port="${REVIX_DB_PORT:-5432}"
        local db_name="${REVIX_DB_NAME:-revix}"
        local db_user="${REVIX_DB_USER:-revix}"
        local db_pass="$REVIX_DB_PASS"
        
        if PGPASSWORD="$db_pass" pg_dump -h "$db_host" -p "$db_port" -U "$db_user" "$db_name" > "$backup_file"; then
            log_success "Pre-import backup created: $backup_file"
        else
            log_warning "Failed to create pre-import backup"
        fi
    else
        cd "$compose_dir"
        if $DOCKER_COMPOSE_CMD exec -T db pg_dump -U revix revix > "$backup_file"; then
            log_success "Pre-import backup created: $backup_file"
        else
            log_warning "Failed to create pre-import backup"
        fi
    fi
}

import_via_docker() {
    local sql_file="$1"
    local clean_import="$2"
    local no_owner="$3"
    local verbose="$4"
    local compose_dir="$5"
    
    cd "$compose_dir"
    
    # Build psql command
    local psql_cmd="psql -U revix -d revix"
    
    if [ "$no_owner" = "true" ]; then
        # For psql, we'll handle this differently since --no-owner is pg_restore specific
        log_info "Note: --no-owner option is mainly for pg_restore. Proceeding with standard import."
    fi
    
    if [ "$verbose" = "true" ]; then
        psql_cmd="$psql_cmd -v ON_ERROR_STOP=1"
    else
        psql_cmd="$psql_cmd -q"
    fi
    
    # Clean database if requested
    if [ "$clean_import" = "true" ]; then
        log_warning "Dropping all tables (DESTRUCTIVE OPERATION)..."
        $DOCKER_COMPOSE_CMD exec -T db psql -U revix -d revix -c "
            DROP SCHEMA public CASCADE;
            CREATE SCHEMA public;
            GRANT ALL ON SCHEMA public TO revix;
            GRANT ALL ON SCHEMA public TO public;
        " || log_warning "Failed to clean database - continuing anyway"
    fi
    
    log_info "Importing database..."
    
    # Handle compressed files
    if [[ "$sql_file" == *.gz ]]; then
        if gunzip -c "$sql_file" | $DOCKER_COMPOSE_CMD exec -T db bash -c "$psql_cmd"; then
            log_success "Database imported successfully from compressed file"
        else
            log_error "Import failed"
            return 1
        fi
    else
        if $DOCKER_COMPOSE_CMD exec -T db bash -c "$psql_cmd" < "$sql_file"; then
            log_success "Database imported successfully"
        else
            log_error "Import failed"
            return 1
        fi
    fi
}

import_via_direct() {
    local sql_file="$1"
    local clean_import="$2"
    local no_owner="$3"
    local verbose="$4"
    
    # Set defaults for direct connection
    local db_host="${REVIX_DB_HOST:-localhost}"
    local db_port="${REVIX_DB_PORT:-5432}"
    local db_name="${REVIX_DB_NAME:-revix}"
    local db_user="${REVIX_DB_USER:-revix}"
    local db_pass="$REVIX_DB_PASS"
    
    if [ -z "$db_pass" ]; then
        log_error "REVIX_DB_PASS environment variable is required for direct connection"
        return 1
    fi
    
    # Test connection
    if ! PGPASSWORD="$db_pass" pg_isready -h "$db_host" -p "$db_port" -U "$db_user" -d "$db_name" > /dev/null 2>&1; then
        log_error "Cannot connect to database at $db_host:$db_port"
        return 1
    fi
    
    # Build psql command
    local psql_cmd="psql -h $db_host -p $db_port -U $db_user -d $db_name"
    
    if [ "$verbose" = "true" ]; then
        psql_cmd="$psql_cmd -v ON_ERROR_STOP=1"
    else
        psql_cmd="$psql_cmd -q"
    fi
    
    # Clean database if requested
    if [ "$clean_import" = "true" ]; then
        log_warning "Dropping all tables (DESTRUCTIVE OPERATION)..."
        PGPASSWORD="$db_pass" psql -h "$db_host" -p "$db_port" -U "$db_user" -d "$db_name" -c "
            DROP SCHEMA public CASCADE;
            CREATE SCHEMA public;
            GRANT ALL ON SCHEMA public TO $db_user;
            GRANT ALL ON SCHEMA public TO public;
        " || log_warning "Failed to clean database - continuing anyway"
    fi
    
    log_info "Importing database via direct connection..."
    
    # Handle compressed files
    if [[ "$sql_file" == *.gz ]]; then
        if gunzip -c "$sql_file" | PGPASSWORD="$db_pass" $psql_cmd; then
            log_success "Database imported successfully from compressed file"
        else
            log_error "Import failed"
            return 1
        fi
    else
        if PGPASSWORD="$db_pass" $psql_cmd < "$sql_file"; then
            log_success "Database imported successfully"
        else
            log_error "Import failed"
            return 1
        fi
    fi
}

confirm_import() {
    local sql_file="$1"
    local clean_import="$2"
    local skip_confirm="$3"
    
    if [ "$skip_confirm" = "true" ]; then
        return 0
    fi
    
    echo ""
    log_warning "=== DATABASE IMPORT CONFIRMATION ==="
    log_info "SQL file: $sql_file"
    
    if [ "$clean_import" = "true" ]; then
        log_warning "Clean import: YES (will delete ALL existing data)"
    else
        log_info "Clean import: NO (will merge with existing data)"
    fi
    
    echo ""
    log_warning "This operation will modify your database."
    log_warning "Make sure you have a backup of your current data."
    echo ""
    
    read -p "Are you sure you want to continue? (yes/NO): " -r
    if [[ ! $REPLY == "yes" ]]; then
        log_info "Import cancelled by user"
        exit 0
    fi
}

main() {
    local sql_file=""
    local clean_import="false"
    local no_owner="false"
    local verbose="false"
    local use_direct="false"
    local compose_dir=""
    local skip_confirm="false"
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--clean)
                clean_import="true"
                shift
                ;;
            -n|--no-owner)
                no_owner="true"
                shift
                ;;
            -v|--verbose)
                verbose="true"
                shift
                ;;
            --docker-compose)
                compose_dir="$2"
                shift 2
                ;;
            --direct)
                use_direct="true"
                shift
                ;;
            --confirm)
                skip_confirm="true"
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [ -z "$sql_file" ]; then
                    sql_file="$1"
                else
                    log_error "Multiple SQL files specified. Please provide only one file."
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # Validate required arguments
    if [ -z "$sql_file" ]; then
        log_error "SQL file is required"
        show_usage
        exit 1
    fi
    
    log_info "Revix Database Import"
    
    # Validate SQL file
    validate_sql_file "$sql_file" || exit 1
    
    # Get confirmation before proceeding
    confirm_import "$sql_file" "$clean_import" "$skip_confirm"
    
    if [ "$use_direct" = "true" ]; then
        # Check if psql is available
        if ! command -v psql &> /dev/null; then
            log_error "psql command not found. Install PostgreSQL client tools."
            exit 1
        fi
        
        # Create backup before import
        create_backup_before_import "" "true"
        
        import_via_direct "$sql_file" "$clean_import" "$no_owner" "$verbose"
    else
        # Docker-based import
        if [ -z "$compose_dir" ]; then
            compose_dir="$PROJECT_ROOT/deploy/docker"
        fi
        
        detect_docker_compose || exit 1
        check_docker_environment "$compose_dir" || exit 1
        
        # Create backup before import
        create_backup_before_import "$compose_dir" "false"
        
        import_via_docker "$sql_file" "$clean_import" "$no_owner" "$verbose" "$compose_dir"
    fi
    
    # Final verification
    log_info "Verifying import..."
    if [ "$use_direct" = "true" ]; then
        local db_host="${REVIX_DB_HOST:-localhost}"
        local db_port="${REVIX_DB_PORT:-5432}"
        local db_name="${REVIX_DB_NAME:-revix}"
        local db_user="${REVIX_DB_USER:-revix}"
        local db_pass="$REVIX_DB_PASS"
        
        local table_count=$(PGPASSWORD="$db_pass" psql -h "$db_host" -p "$db_port" -U "$db_user" -d "$db_name" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' ')
    else
        cd "$compose_dir"
        local table_count=$($DOCKER_COMPOSE_CMD exec -T db psql -U revix -d revix -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' ')
    fi
    
    if [ "$table_count" -gt 0 ]; then
        log_success "Import verification passed - found $table_count tables"
        log_info "Database import completed successfully"
    else
        log_warning "Import verification failed - no tables found"
        log_warning "Check the import logs for errors"
    fi
}

# Run main function with all arguments
main "$@"