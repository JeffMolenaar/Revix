# Revix Installation Guide

Revix is a self-hosted car maintenance tracking platform built with Kotlin Multiplatform.

## Quick Start with Docker

### Prerequisites
- Docker and Docker Compose
- At least 2GB of available RAM

### 1. Clone the Repository
```bash
git clone https://github.com/JeffMolenaar/Revix.git
cd Revix
```

### 2. Build and Start the Services
```bash
# Option 1: Use the build script (recommended)
./build.sh

# Option 2: Manual build
./gradlew server:installDist
cd deploy/docker
docker compose up -d
```

This will start:
- PostgreSQL database on port 5432
- Revix API server on port 8080

### 3. Verify Installation
```bash
curl http://localhost:8080/health
```

You should see: `{"status":"ok","timestamp":...}`

### 4. Create Your First User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "password": "YourSecurePassword123",
    "name": "Your Name"
  }'
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Create a new user account
- `POST /api/v1/auth/login` - Login with email/password
- `POST /api/v1/auth/refresh` - Refresh access token
- `GET /api/v1/auth/me` - Get current user profile

### Vehicles
- `GET /api/v1/vehicles` - List your vehicles
- `POST /api/v1/vehicles` - Add a new vehicle
- `GET /api/v1/vehicles/{id}` - Get vehicle details
- `PUT /api/v1/vehicles/{id}` - Update vehicle
- `DELETE /api/v1/vehicles/{id}` - Delete vehicle

### Parts
- `GET /api/v1/parts` - List parts (supports filtering by tags and search)
- `POST /api/v1/parts` - Add a new part
- `GET /api/v1/parts/{id}` - Get part details
- `PUT /api/v1/parts/{id}` - Update part
- `DELETE /api/v1/parts/{id}` - Delete part

### Tags
- `GET /api/v1/tags` - List your tags
- `POST /api/v1/tags` - Create a new tag
- `PUT /api/v1/tags/{id}` - Update tag
- `DELETE /api/v1/tags/{id}` - Delete tag

### Maintenance Records
- `GET /api/v1/vehicles/{vehicleId}/maintenance` - List maintenance records for a vehicle
- `POST /api/v1/vehicles/{vehicleId}/maintenance` - Create maintenance record
- `GET /api/v1/maintenance/{id}` - Get maintenance record details
- `PUT /api/v1/maintenance/{id}` - Update maintenance record
- `DELETE /api/v1/maintenance/{id}` - Delete maintenance record

## Environment Variables

### Required
- `REVIX_JWT_SECRET` - JWT signing secret (use a long random string in production)

### Optional
- `REVIX_DB_URL` - Database connection URL (default: jdbc:postgresql://localhost:5432/revix)
- `REVIX_DB_USER` - Database username (default: revix)
- `REVIX_DB_PASS` - Database password (default: revix)
- `REVIX_PORT` - Server port (default: 8080)
- `REVIX_HOST` - Server host (default: 0.0.0.0)

## Development Setup

### Prerequisites
- JDK 17+
- Gradle 8.5+
- PostgreSQL 16+

### 1. Setup Database
```bash
# Create database
createdb revix
```

### 2. Set Environment Variables
```bash
export REVIX_DB_URL=jdbc:postgresql://localhost:5432/revix
export REVIX_DB_USER=your_username
export REVIX_DB_PASS=your_password
export REVIX_JWT_SECRET=your-development-secret
```

### 3. Build and Run
```bash
./gradlew server:run
```

The server will start on http://localhost:8080

### 4. Database Migrations
Migrations run automatically on startup using Flyway.

## Security Considerations

### Production Deployment
1. **Change the JWT secret**: Use a long, random secret
2. **Use HTTPS**: Configure a reverse proxy (nginx/traefik) with SSL
3. **Database security**: Use strong passwords and restrict network access
4. **Regular updates**: Keep dependencies and base images updated
5. **Monitoring**: Set up logging and monitoring

### JWT Token Management
- Access tokens expire after 15 minutes
- Refresh tokens expire after 7 days
- Tokens are automatically cleaned up on expiration

## Backup and Restore

Revix provides comprehensive database import/export functionality that works both locally and via SSH connections.

### Quick Backup and Restore (Docker)
```bash
# Quick backup
docker-compose exec db pg_dump -U revix revix > backup.sql

# Quick restore
docker-compose exec -T db psql -U revix revix < backup.sql
```

### SSH-Accessible Database Management

For remote management via SSH (e.g., using PuTTY), use the dedicated scripts:

#### Database Export via SSH
```bash
# Basic export
./scripts/db-export.sh

# Export to specific file
./scripts/db-export.sh -f my_backup.sql

# Export and compress
./scripts/db-export.sh -c -f backup.sql

# Schema only export
./scripts/db-export.sh --schema-only -f schema.sql

# Export to custom directory
./scripts/db-export.sh -d /path/to/backups -f backup.sql
```

#### Database Import via SSH
```bash
# Basic import (with confirmation prompt)
./scripts/db-import.sh backup.sql

# Import compressed file
./scripts/db-import.sh backup.sql.gz

# Clean import (removes existing data)
./scripts/db-import.sh --clean backup.sql

# Skip confirmation prompt (use with caution)
./scripts/db-import.sh --confirm backup.sql
```

### Direct PostgreSQL Connection

For environments without Docker, you can use direct PostgreSQL connections:

```bash
# Set environment variables
export REVIX_DB_HOST=localhost
export REVIX_DB_PORT=5432
export REVIX_DB_NAME=revix
export REVIX_DB_USER=revix
export REVIX_DB_PASS=your_password

# Export using direct connection
./scripts/db-export.sh --direct

# Import using direct connection
./scripts/db-import.sh --direct backup.sql
```

### SSH Connection Examples

#### Using PuTTY or SSH Terminal
```bash
# Connect to your server
ssh user@your-server.com

# Navigate to Revix directory
cd /path/to/Revix

# Export database
./scripts/db-export.sh -c -f "backup_$(date +%Y%m%d).sql"

# Download the backup file using SCP
# (run this from your local machine)
scp user@your-server.com:/path/to/Revix/backups/backup_20240905.sql.gz ./

# Upload and import a backup file
# (upload file first, then import)
scp ./my_backup.sql user@your-server.com:/path/to/Revix/backups/
ssh user@your-server.com
cd /path/to/Revix
./scripts/db-import.sh backups/my_backup.sql
```

### Important Security Notes

- **Backup Files**: Keep backup files secure and delete when no longer needed
- **SSH Access**: Ensure proper SSH key authentication and firewall rules
- **Database Passwords**: Use strong passwords and secure environment variables
- **File Permissions**: Backup files may contain sensitive data - set appropriate permissions
- **Clean Imports**: The `--clean` option will delete ALL existing data - use with extreme caution

### Script Features

- **Auto-backup**: Import script automatically creates a backup before importing
- **Compression**: Support for gzip-compressed SQL files
- **Validation**: File format validation and database connectivity checks
- **Error Handling**: Comprehensive error checking and user confirmations
- **Flexible Output**: Customizable backup locations and filenames

## Troubleshooting

### Server won't start
1. Check Docker logs: `docker-compose logs server`
2. Verify database connection
3. Check environment variables

### Database connection issues
1. Ensure PostgreSQL is running: `docker-compose ps`
2. Check database health: `docker-compose exec db pg_isready -U revix`
3. Verify connection string format

### API returns 500 errors
1. Check server logs for stack traces
2. Verify JWT secret is set
3. Check database migrations completed successfully

## Customization

### CSS Styling
You can customize the appearance of Revix by editing CSS files. See the [CSS Customization Guide](CSS-CUSTOMIZATION.md) for detailed instructions on:
- How to edit CSS via SSH or file system
- Available CSS variables and theming options
- Examples for common customizations (dark theme, colors, fonts)
- Troubleshooting styling issues

## Support

For issues and feature requests, please create an issue on the GitHub repository.