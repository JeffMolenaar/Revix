#!/bin/bash

# Revix Complete Installation Script for Ubuntu Server LTS 24.04
# Run as root: sudo bash install-ubuntu.sh
# Or as root user: bash install-ubuntu.sh

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

# Check if running as root
check_root() {
    if [ "$EUID" -ne 0 ]; then
        log_error "This script must be run as root. Use: sudo bash install-ubuntu.sh"
        exit 1
    fi
    log_success "Running as root user"
}

# Update system packages
update_system() {
    log_info "Updating system packages..."
    apt-get update -y
    apt-get upgrade -y
    log_success "System packages updated"
}

# Install basic dependencies
install_dependencies() {
    log_info "Installing basic dependencies..."
    apt-get install -y \
        curl \
        wget \
        git \
        unzip \
        software-properties-common \
        apt-transport-https \
        ca-certificates \
        gnupg \
        lsb-release \
        build-essential \
        jq
    log_success "Basic dependencies installed"
}

# Install Java 17
install_java() {
    log_info "Installing OpenJDK 17..."
    apt-get install -y openjdk-17-jdk
    
    # Verify Java installation
    java_version=$(java -version 2>&1 | grep "openjdk version" | cut -d'"' -f2)
    log_success "Java installed: $java_version"
    
    # Set JAVA_HOME
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> /root/.bashrc
}

# Install Docker
install_docker() {
    log_info "Installing Docker..."
    
    # Remove old Docker versions
    apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true
    
    # Add Docker's official GPG key
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    
    # Add Docker repository
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    # Update package list and install Docker
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    # Start and enable Docker
    systemctl start docker
    systemctl enable docker
    
    # Add docker group (for non-root usage later if needed)
    groupadd docker 2>/dev/null || true
    
    # Verify Docker installation
    docker_version=$(docker --version)
    log_success "Docker installed: $docker_version"
}

# Install Docker Compose (use plugin included with Docker)
install_docker_compose() {
    log_info "Verifying Docker Compose installation..."
    
    # Docker Compose plugin should already be installed with docker-compose-plugin
    # Let's verify and set up the right command
    if command -v docker-compose &> /dev/null; then
        compose_version=$(docker-compose --version)
        log_success "Docker Compose (standalone) found: $compose_version"
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        compose_version=$(docker compose version)
        log_success "Docker Compose (plugin) found: $compose_version"
        DOCKER_COMPOSE_CMD="docker compose"
        
        # Create a compatibility symlink for docker-compose command
        if [ ! -f /usr/local/bin/docker-compose ]; then
            log_info "Creating docker-compose compatibility script..."
            cat > /usr/local/bin/docker-compose << 'EOF'
#!/bin/bash
exec docker compose "$@"
EOF
            chmod +x /usr/local/bin/docker-compose
            log_success "Created docker-compose compatibility script"
        fi
    else
        log_error "Docker Compose not found. Trying to install standalone version..."
        # Fallback to standalone installation
        COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | jq -r .tag_name)
        curl -L "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        chmod +x /usr/local/bin/docker-compose
        compose_version=$(docker-compose --version)
        log_success "Docker Compose (standalone) installed: $compose_version"
        DOCKER_COMPOSE_CMD="docker-compose"
    fi
}

# Cleanup old installation
cleanup_old_installation() {
    log_info "Cleaning up any existing Revix installation..."
    
    local repo_dir="/root/Revix"
    
    # Stop any running containers first
    if [ -d "$repo_dir/deploy/docker" ]; then
        cd "$repo_dir/deploy/docker"
        log_info "Stopping existing Revix containers..."
        $DOCKER_COMPOSE_CMD down --volumes --remove-orphans 2>/dev/null || true
        docker compose down --volumes --remove-orphans 2>/dev/null || true
        docker-compose down --volumes --remove-orphans 2>/dev/null || true
    fi
    
    # Remove containers and images related to Revix
    log_info "Removing Revix Docker containers and images..."
    docker ps -a --format "table {{.Names}}" | grep -E "(revix|docker_server|docker_db)" | xargs -r docker rm -f 2>/dev/null || true
    docker images --format "table {{.Repository}}:{{.Tag}}" | grep -E "(revix|docker_server|docker_db)" | xargs -r docker rmi -f 2>/dev/null || true
    
    # Remove Docker volumes
    docker volume ls --format "table {{.Name}}" | grep -E "(revix|docker_revix)" | xargs -r docker volume rm -f 2>/dev/null || true
    
    # Clean up repository directory
    if [ -d "$repo_dir" ]; then
        log_info "Removing old repository directory..."
        rm -rf "$repo_dir"
    fi
    
    # Clean up any temporary files
    rm -rf /tmp/revix* 2>/dev/null || true
    
    log_success "Cleanup completed"
}

# Clone or update Revix repository
setup_repository() {
    log_info "Setting up Revix repository..."
    local repo_dir="/root/Revix"
    
    # Clone the repository (cleanup should have removed any existing directory)
    log_info "Cloning Revix repository..."
    git clone https://github.com/JeffMolenaar/Revix.git "$repo_dir"
    cd "$repo_dir"
    
    log_success "Repository ready at $repo_dir"
}

# Build the application
build_application() {
    log_info "Building Revix application..."
    
    # Make gradlew executable
    chmod +x gradlew
    
    # Build the application
    log_info "Running Gradle build..."
    ./gradlew clean server:installDist
    
    # Verify build succeeded
    if [ ! -d "server/build/install/server" ]; then
        log_error "Build failed. Distribution not found at server/build/install/server"
        exit 1
    fi
    
    log_success "Application built successfully"
}

# Configure environment
configure_environment() {
    log_info "Configuring environment..."
    
    # Generate a secure JWT secret
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    
    # Create environment file for docker-compose
    cat > deploy/docker/.env << EOF
REVIX_DB_URL=jdbc:postgresql://db:5432/revix
REVIX_DB_USER=revix
REVIX_DB_PASS=revix_$(openssl rand -base64 12 | tr -d '\n')
REVIX_JWT_SECRET=$JWT_SECRET
REVIX_BASE_URL=http://localhost:8080
EOF

    log_success "Environment configured with secure secrets"
}

# Start services
start_services() {
    log_info "Starting Revix services..."
    
    cd deploy/docker
    
    # Stop any existing services
    $DOCKER_COMPOSE_CMD down 2>/dev/null || true
    
    # Build and start services
    $DOCKER_COMPOSE_CMD build server
    $DOCKER_COMPOSE_CMD up -d
    
    log_info "Waiting for services to start..."
    sleep 10
    
    # Wait for health check
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        log_info "Checking health (attempt $attempt/$max_attempts)..."
        
        # Try to get health response
        health_response=$(curl -s http://localhost:8080/health 2>/dev/null)
        curl_exit_code=$?
        
        if [ $curl_exit_code -eq 0 ] && echo "$health_response" | grep -q '"status"[[:space:]]*:[[:space:]]*"ok"'; then
            log_success "Services are healthy! Response: $health_response"
            break
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            log_error "Services failed to start after $max_attempts attempts"
            if [ $curl_exit_code -ne 0 ]; then
                log_error "Could not connect to health endpoint (curl exit code: $curl_exit_code)"
            else
                log_error "Health endpoint returned unexpected response: $health_response"
            fi
            log_info "Checking service status..."
            $DOCKER_COMPOSE_CMD ps
            log_info "Checking server logs..."
            $DOCKER_COMPOSE_CMD logs server | tail -20
            exit 1
        fi
        
        sleep 5
        ((attempt++))
    done
}

# Verify installation
verify_installation() {
    log_info "Verifying installation..."
    
    # Check health endpoint
    health_response=$(curl -s http://localhost:8080/health)
    if echo "$health_response" | grep -q '"status"[[:space:]]*:[[:space:]]*"ok"'; then
        log_success "Health check passed: $health_response"
    else
        log_error "Health check failed: $health_response"
        exit 1
    fi
    
    # Check service status
    cd deploy/docker
    log_info "Service status:"
    $DOCKER_COMPOSE_CMD ps
    
    log_success "Installation verification completed successfully!"
}

# Setup firewall (optional)
setup_firewall() {
    log_info "Configuring firewall..."
    
    # Enable UFW if not already enabled
    ufw --force enable 2>/dev/null || true
    
    # Allow SSH (important!)
    ufw allow ssh
    
    # Allow Revix port
    ufw allow 8080
    
    # Show status
    ufw status
    
    log_success "Firewall configured"
}

# Print final instructions
print_instructions() {
    echo ""
    echo "=================================================================="
    log_success "🎉 Revix installation completed successfully!"
    echo "=================================================================="
    echo ""
    echo "📊 Service URLs:"
    echo "   • Health Check: http://localhost:8080/health"
    echo "   • API Base: http://localhost:8080/api/v1"
    echo ""
    echo "🔧 Managing Services:"
    echo "   • Start: cd /root/Revix/deploy/docker && docker compose up -d"
    echo "   • Stop: cd /root/Revix/deploy/docker && docker compose down"
    echo "   • Logs: cd /root/Revix/deploy/docker && docker compose logs -f"
    echo "   • Status: cd /root/Revix/deploy/docker && docker compose ps"
    echo ""
    echo "👤 Create your first user:"
    echo '   curl -X POST http://localhost:8080/api/v1/auth/register \'
    echo '     -H "Content-Type: application/json" \'
    echo '     -d '"'"'{'
    echo '       "email": "admin@example.com",'
    echo '       "password": "SecurePassword123",'
    echo '       "name": "Admin User"'
    echo '     }'"'"
    echo ""
    echo "📚 Documentation: /root/Revix/docs/"
    echo ""
    echo "🔒 Security Notes:"
    echo "   • JWT secret has been generated automatically"
    echo "   • Database password has been generated automatically"
    echo "   • Remember to configure HTTPS for production use"
    echo "   • Consider setting up regular backups"
    echo ""
    log_warning "For external access, make sure port 8080 is open in your server's firewall"
    echo ""
}

# Main installation function
main() {
    echo "=================================================================="
    echo "🚗 Revix Installation Script for Ubuntu Server LTS 24.04"
    echo "=================================================================="
    echo ""
    
    check_root
    update_system
    install_dependencies
    install_java
    install_docker
    install_docker_compose
    cleanup_old_installation
    setup_repository
    build_application
    configure_environment
    start_services
    verify_installation
    setup_firewall
    print_instructions
}

# Run main function
main "$@"