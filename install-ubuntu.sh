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

# Install Docker Compose (standalone for compatibility)
install_docker_compose() {
    log_info "Installing Docker Compose standalone..."
    
    # Download latest Docker Compose
    COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | jq -r .tag_name)
    curl -L "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    
    # Make it executable
    chmod +x /usr/local/bin/docker-compose
    
    # Create symlink for compatibility
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    
    # Verify installation
    compose_version=$(docker-compose --version)
    log_success "Docker Compose installed: $compose_version"
}

# Clone or update Revix repository
setup_repository() {
    local repo_dir="/root/Revix"
    
    if [ -d "$repo_dir" ]; then
        log_info "Revix repository already exists at $repo_dir"
        cd "$repo_dir"
        
        # Check if it's a git repository
        if [ -d ".git" ]; then
            log_info "Updating existing repository..."
            git fetch origin
            git reset --hard origin/main 2>/dev/null || git reset --hard origin/master 2>/dev/null || log_warning "Could not reset to latest commit"
        else
            log_warning "Directory exists but is not a git repository. Backing up and re-cloning..."
            mv "$repo_dir" "${repo_dir}.backup.$(date +%s)"
            git clone https://github.com/JeffMolenaar/Revix.git "$repo_dir"
            cd "$repo_dir"
        fi
    else
        log_info "Cloning Revix repository..."
        git clone https://github.com/JeffMolenaar/Revix.git "$repo_dir"
        cd "$repo_dir"
    fi
    
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
    docker-compose down 2>/dev/null || true
    
    # Build and start services
    docker-compose build server
    docker-compose up -d
    
    log_info "Waiting for services to start..."
    sleep 10
    
    # Wait for health check
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        log_info "Checking health (attempt $attempt/$max_attempts)..."
        
        if curl -f -s http://localhost:8080/health > /dev/null 2>&1; then
            log_success "Services are healthy!"
            break
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            log_error "Services failed to start after $max_attempts attempts"
            log_info "Checking service status..."
            docker-compose ps
            log_info "Checking server logs..."
            docker-compose logs server | tail -20
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
    if echo "$health_response" | grep -q '"status":"ok"'; then
        log_success "Health check passed: $health_response"
    else
        log_error "Health check failed: $health_response"
        exit 1
    fi
    
    # Check service status
    cd deploy/docker
    log_info "Service status:"
    docker-compose ps
    
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
    echo "   • Start: cd /root/Revix/deploy/docker && docker-compose up -d"
    echo "   • Stop: cd /root/Revix/deploy/docker && docker-compose down"
    echo "   • Logs: cd /root/Revix/deploy/docker && docker-compose logs -f"
    echo "   • Status: cd /root/Revix/deploy/docker && docker-compose ps"
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