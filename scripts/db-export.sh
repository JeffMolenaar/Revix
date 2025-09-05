#!/bin/bash

# Revix Database Export Script
# This script exports the database data to an SQL file for backup purposes
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
EXPORT_DIR="$PROJECT_ROOT/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DEFAULT_BACKUP_FILE="revix_backup_$TIMESTAMP.sql"

# Docker Compose command detection
DOCKER_COMPOSE_CMD=""

show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Export Revix database to SQL file"
    echo ""
    echo "Options:"
    echo "  -f, --file FILENAME     Output SQL file name (default: revix_backup_TIMESTAMP.sql)"
    echo "  -d, --directory DIR     Output directory (default: $PROJECT_ROOT/backups)"
    echo "  -c, --compress          Compress the output file with gzip"
    echo "  -s, --schema-only       Export schema only (no data)"
    echo "  -D, --data-only         Export data only (no schema)"
    echo "  --docker-compose DIR    Docker compose directory (default: auto-detect)"
    echo "  --direct                Use direct PostgreSQL connection (requires credentials)"
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
    echo "  $0                                    # Export with default settings"
    echo "  $0 -f my_backup.sql                  # Export to specific file"
    echo "  $0 -c -f backup.sql                  # Export and compress"
    echo "  $0 --schema-only -f schema.sql       # Export schema only"
    echo "  $0 --direct                          # Use direct PostgreSQL connection"
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

export_via_docker() {
    local output_file="$1"
    local compress="$2"
    local schema_only="$3"
    local data_only="$4"
    local compose_dir="$5"
    
    cd "$compose_dir"
    
    # Build pg_dump command
    local pg_dump_cmd="pg_dump -U revix -h localhost revix"
    
    if [ "$schema_only" = "true" ]; then
        pg_dump_cmd="$pg_dump_cmd --schema-only"
    elif [ "$data_only" = "true" ]; then
        pg_dump_cmd="$pg_dump_cmd --data-only"
    fi
    
    log_info "Exporting database..."
    
    if [ "$compress" = "true" ]; then
        if $DOCKER_COMPOSE_CMD exec -T db bash -c "$pg_dump_cmd" | gzip > "$output_file.gz"; then
            log_success "Database exported and compressed to: $output_file.gz"
        else
            log_error "Export failed"
            return 1
        fi
    else
        if $DOCKER_COMPOSE_CMD exec -T db bash -c "$pg_dump_cmd" > "$output_file"; then
            log_success "Database exported to: $output_file"
        else
            log_error "Export failed"
            return 1
        fi
    fi
}

export_via_direct() {
    local output_file="$1"
    local compress="$2"
    local schema_only="$3"
    local data_only="$4"
    
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
    
    # Build pg_dump command
    local pg_dump_cmd="pg_dump -h $db_host -p $db_port -U $db_user $db_name"
    
    if [ "$schema_only" = "true" ]; then
        pg_dump_cmd="$pg_dump_cmd --schema-only"
    elif [ "$data_only" = "true" ]; then
        pg_dump_cmd="$pg_dump_cmd --data-only"
    fi
    
    log_info "Exporting database via direct connection..."
    
    if [ "$compress" = "true" ]; then
        if PGPASSWORD="$db_pass" $pg_dump_cmd | gzip > "$output_file.gz"; then
            log_success "Database exported and compressed to: $output_file.gz"
        else
            log_error "Export failed"
            return 1
        fi
    else
        if PGPASSWORD="$db_pass" $pg_dump_cmd > "$output_file"; then
            log_success "Database exported to: $output_file"
        else
            log_error "Export failed"
            return 1
        fi
    fi
}

main() {
    local output_file="$DEFAULT_BACKUP_FILE"
    local output_dir="$EXPORT_DIR"
    local compress="false"
    local schema_only="false"
    local data_only="false"
    local use_direct="false"
    local compose_dir=""
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -f|--file)
                output_file="$2"
                shift 2
                ;;
            -d|--directory)
                output_dir="$2"
                shift 2
                ;;
            -c|--compress)
                compress="true"
                shift
                ;;
            -s|--schema-only)
                schema_only="true"
                shift
                ;;
            -D|--data-only)
                data_only="true"
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
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Validate conflicting options
    if [ "$schema_only" = "true" ] && [ "$data_only" = "true" ]; then
        log_error "Cannot use --schema-only and --data-only together"
        exit 1
    fi
    
    # Create output directory
    mkdir -p "$output_dir"
    
    # Full path to output file
    local full_output_path="$output_dir/$output_file"
    
    log_info "Revix Database Export"
    log_info "Output directory: $output_dir"
    log_info "Output file: $output_file"
    
    if [ "$use_direct" = "true" ]; then
        # Check if pg_dump is available
        if ! command -v pg_dump &> /dev/null; then
            log_error "pg_dump command not found. Install PostgreSQL client tools."
            exit 1
        fi
        
        export_via_direct "$full_output_path" "$compress" "$schema_only" "$data_only"
    else
        # Docker-based export
        if [ -z "$compose_dir" ]; then
            compose_dir="$PROJECT_ROOT/deploy/docker"
        fi
        
        detect_docker_compose || exit 1
        check_docker_environment "$compose_dir" || exit 1
        export_via_docker "$full_output_path" "$compress" "$schema_only" "$data_only" "$compose_dir"
    fi
    
    # Show file information
    if [ "$compress" = "true" ]; then
        local final_file="$full_output_path.gz"
    else
        local final_file="$full_output_path"
    fi
    
    if [ -f "$final_file" ]; then
        local file_size=$(du -h "$final_file" | cut -f1)
        log_info "Export completed successfully"
        log_info "File size: $file_size"
        log_info "Location: $final_file"
        
        # Security note
        log_warning "Keep backup files secure and delete when no longer needed"
    else
        log_error "Export failed - output file not created"
        exit 1
    fi
}

# Run main function with all arguments
main "$@"