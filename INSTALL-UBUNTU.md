# Revix Ubuntu Installation Script

This script provides a complete, automated installation of Revix on Ubuntu Server LTS 24.04.

## Quick Install

Run as root:

```bash
# Download and run the installation script
curl -fsSL https://raw.githubusercontent.com/JeffMolenaar/Revix/main/install-ubuntu.sh | bash
```

Or clone the repository first:

```bash
git clone https://github.com/JeffMolenaar/Revix.git
cd Revix
sudo bash install-ubuntu.sh
```

## What the Script Does

The installation script automatically:

1. **Updates System**: Updates Ubuntu packages to latest versions
2. **Installs Dependencies**: Installs required packages (curl, git, build tools, etc.)
3. **Installs Java 17**: Installs OpenJDK 17 required for building the application
4. **Installs Docker**: Installs Docker CE and Docker Compose
5. **Clones Repository**: Downloads the Revix source code
6. **Builds Application**: Compiles the Kotlin application using Gradle
7. **Configures Environment**: Generates secure JWT secrets and database passwords
8. **Starts Services**: Launches PostgreSQL database and Revix API server
9. **Verifies Installation**: Tests that all services are running correctly
10. **Configures Firewall**: Opens necessary ports (SSH and 8080)

## System Requirements

- **OS**: Ubuntu Server LTS 24.04
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 10GB available disk space
- **Network**: Internet connection for downloading dependencies
- **Privileges**: Root access required

## After Installation

Once the script completes successfully:

### Service Management

```bash
# Check service status
cd /root/Revix/deploy/docker
docker-compose ps

# View logs
docker-compose logs -f server

# Restart services
docker-compose restart

# Stop services
docker-compose down

# Start services
docker-compose up -d
```

### Create Your First User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "SecurePassword123",
    "name": "Admin User"
  }'
```

### Test the API

```bash
# Health check
curl http://localhost:8080/health

# Should return: {"status":"ok","timestamp":"..."}
```

## Security Considerations

The installation script automatically:

- Generates a secure random JWT secret
- Creates a secure database password
- Configures basic firewall rules
- Uses latest stable versions of all components

For production use, consider:

1. **HTTPS Setup**: Configure a reverse proxy with SSL certificates
2. **Regular Updates**: Keep the system and Docker images updated
3. **Backup Strategy**: Set up automated database backups
4. **Monitor Logs**: Set up log monitoring and alerting
5. **Network Security**: Restrict access to necessary ports only

## Troubleshooting

### Installation Fails

1. Check you're running as root: `whoami` should return `root`
2. Ensure internet connectivity: `ping google.com`
3. Check disk space: `df -h`
4. Review installation logs for specific error messages

### Services Won't Start

1. Check Docker status: `systemctl status docker`
2. View service logs: `cd /root/Revix/deploy/docker && docker-compose logs`
3. Check port availability: `netstat -tlnp | grep 8080`

### Health Check Fails

1. Wait 30-60 seconds for services to fully start
2. Check if containers are running: `docker-compose ps`
3. View server logs: `docker-compose logs server`

### Can't Access from External Network

1. Check firewall: `ufw status`
2. Ensure port 8080 is open on your cloud provider/router
3. Verify service is listening on all interfaces: `netstat -tlnp | grep 8080`

## Manual Installation

If you prefer to install manually or need to customize the process, see:

- [docs/INSTALL.md](docs/INSTALL.md) - Detailed manual installation guide
- [deploy/docker/README.md](deploy/docker/README.md) - Docker-specific instructions

## Support

- 📖 [Installation Guide](docs/INSTALL.md)
- 🐛 [Issue Tracker](https://github.com/JeffMolenaar/Revix/issues)
- 💬 [Discussions](https://github.com/JeffMolenaar/Revix/discussions)