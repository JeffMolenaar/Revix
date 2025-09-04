#!/bin/bash

# Revix Installation Validation Script
# Run this script to verify your Revix installation is working correctly

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
DOCKER_COMPOSE_CMD=""

check_docker() {
    log_info "Checking Docker installation..."
    
    if command -v docker &> /dev/null; then
        docker_version=$(docker --version)
        log_success "Docker found: $docker_version"
    else
        log_error "Docker not found. Please install Docker first."
        return 1
    fi
    
    if command -v docker-compose &> /dev/null; then
        compose_version=$(docker-compose --version)
        log_success "Docker Compose found: $compose_version"
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        compose_version=$(docker compose version)
        log_success "Docker Compose found: $compose_version"
        DOCKER_COMPOSE_CMD="docker compose"
    else
        log_error "Docker Compose not found. Please install Docker Compose."
        return 1
    fi
}

check_services() {
    log_info "Checking if services are running..."
    
    # Change to docker directory if it exists
    if [ -d "deploy/docker" ]; then
        cd deploy/docker
    elif [ -d "/root/Revix/deploy/docker" ]; then
        cd /root/Revix/deploy/docker
    else
        log_error "Could not find deploy/docker directory"
        return 1
    fi
    
    # Check if containers are running
    if $DOCKER_COMPOSE_CMD ps | grep -q "Up"; then
        log_success "Docker containers are running"
        $DOCKER_COMPOSE_CMD ps
    else
        log_warning "Containers may not be running. Status:"
        $DOCKER_COMPOSE_CMD ps
    fi
}

check_health() {
    log_info "Checking application health..."
    
    local max_attempts=20
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        # Try to get health response
        health_response=$(curl -s http://localhost:8080/health 2>/dev/null)
        curl_exit_code=$?
        
        if [ $curl_exit_code -eq 0 ] && echo "$health_response" | grep -q '"status"[[:space:]]*:[[:space:]]*"ok"'; then
            log_success "Health check passed: $health_response"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            log_error "Health check failed after $max_attempts attempts"
            if [ $curl_exit_code -ne 0 ]; then
                log_error "Could not connect to http://localhost:8080/health (curl exit code: $curl_exit_code)"
                log_info "Possible causes: service not started, port not accessible, or application not fully initialized"
            else
                log_error "Health endpoint returned unexpected response: $health_response"
            fi
            return 1
        fi
        
        log_info "Health check attempt $attempt/$max_attempts failed, retrying in 5 seconds..."
        sleep 5
        ((attempt++))
    done
}

check_database() {
    log_info "Checking database connectivity..."
    
    # Try to connect to database via Docker
    if $DOCKER_COMPOSE_CMD exec -T db pg_isready -U revix -d revix > /dev/null 2>&1; then
        log_success "Database is accessible"
    else
        log_error "Database connection failed"
        return 1
    fi
}

test_api() {
    log_info "Testing API endpoints..."
    
    # Test health endpoint
    if curl -f -s http://localhost:8080/health | grep -q '"status"[[:space:]]*:[[:space:]]*"ok"'; then
        log_success "Health endpoint working"
    else
        log_error "Health endpoint failed"
        return 1
    fi
    
    # Test registration endpoint (should return error for invalid data, which is expected)
    if curl -f -s -X POST http://localhost:8080/api/v1/auth/register \
        -H "Content-Type: application/json" \
        -d '{}' > /dev/null 2>&1; then
        log_warning "Registration endpoint returned success for empty data (unexpected)"
    else
        log_success "Registration endpoint correctly rejects invalid data"
    fi
}

show_service_info() {
    log_info "Service Information:"
    echo ""
    echo "📊 URLs:"
    echo "   • Health: http://localhost:8080/health"
    echo "   • API Base: http://localhost:8080/api/v1"
    echo ""
    echo "🔧 Management Commands:"
    echo "   • Status: docker compose ps (or docker-compose ps)"
    echo "   • Logs: docker compose logs -f (or docker-compose logs -f)"
    echo "   • Restart: docker compose restart (or docker-compose restart)"
    echo "   • Stop: docker compose down (or docker-compose down)"
    echo "   • Start: docker compose up -d (or docker-compose up -d)"
    echo ""
}

main() {
    echo "=================================================================="
    echo "🔍 Revix Installation Validation"
    echo "=================================================================="
    echo ""
    
    local failed=0
    
    check_docker || ((failed++))
    check_services || ((failed++))
    check_health || ((failed++))
    check_database || ((failed++))
    test_api || ((failed++))
    
    echo ""
    echo "=================================================================="
    
    if [ $failed -eq 0 ]; then
        log_success "🎉 All checks passed! Revix is working correctly."
        show_service_info
    else
        log_error "❌ $failed check(s) failed. Please review the errors above."
        echo ""
        echo "Common troubleshooting steps:"
        echo "1. Ensure services are started: docker compose up -d (or docker-compose up -d)"
        echo "2. Check logs: docker compose logs (or docker-compose logs)"
        echo "3. Restart services: docker compose restart (or docker-compose restart)"
        echo "4. Check firewall: sudo ufw status"
        exit 1
    fi
    
    echo "=================================================================="
}

main "$@"