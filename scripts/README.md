# Revix Database Scripts

This directory contains database management scripts for Revix, designed to work both locally and via SSH connections.

## Scripts

### `db-export.sh` - Database Export Script

Exports Revix database to SQL files with support for compression and various options.

**Usage:**
```bash
./db-export.sh [OPTIONS]
```

**Common Examples:**
```bash
# Basic export
./db-export.sh

# Export with compression
./db-export.sh -c -f backup.sql

# Schema only export
./db-export.sh --schema-only -f schema.sql

# Direct PostgreSQL connection
./db-export.sh --direct
```

### `db-import.sh` - Database Import Script

Imports SQL files to Revix database with validation and safety features.

**Usage:**
```bash
./db-import.sh [OPTIONS] <SQL_FILE>
```

**Common Examples:**
```bash
# Basic import
./db-import.sh backup.sql

# Import compressed file
./db-import.sh backup.sql.gz

# Clean import (removes existing data)
./db-import.sh --clean backup.sql

# Direct PostgreSQL connection
./db-import.sh --direct backup.sql
```

## Features

- **SSH Compatible**: Designed for use over SSH connections (PuTTY, terminal)
- **Docker Support**: Works with Docker Compose environments
- **Direct Connection**: Supports direct PostgreSQL connections
- **Compression**: Gzip compression support for large databases
- **Safety Features**: Automatic backups before import, confirmation prompts
- **Validation**: File format validation and connectivity checks
- **Flexible Options**: Customizable output locations and formats

## Requirements

### For Docker Mode (Default)
- Docker and Docker Compose installed
- Revix database container running

### For Direct Mode
- PostgreSQL client tools (`pg_dump`, `psql`)
- Database connection credentials in environment variables

## Environment Variables (Direct Mode)

```bash
export REVIX_DB_HOST=localhost
export REVIX_DB_PORT=5432
export REVIX_DB_NAME=revix
export REVIX_DB_USER=revix
export REVIX_DB_PASS=your_password
```

## SSH Usage

1. **Connect to server**: `ssh user@server`
2. **Navigate to Revix**: `cd /path/to/Revix`
3. **Run scripts**: `./scripts/db-export.sh` or `./scripts/db-import.sh backup.sql`

## Safety Features

- **Pre-import backups**: Import script automatically creates backups
- **Confirmation prompts**: Interactive confirmation for destructive operations
- **File validation**: SQL file format and compression validation
- **Error handling**: Comprehensive error checking and reporting
- **Help system**: Built-in help with `--help` option

## File Locations

- **Default backup directory**: `./backups/`
- **Backup filename format**: `revix_backup_YYYYMMDD_HHMMSS.sql`
- **Compressed files**: `.sql.gz` extension

## Security Notes

- Keep backup files secure and delete when no longer needed
- Use SSH key authentication for remote access
- Set appropriate file permissions on backup files
- Monitor database access and backup operations

## Troubleshooting

### Common Issues

1. **Permission denied**: Make sure scripts are executable (`chmod +x *.sh`)
2. **Docker not found**: Use `--direct` mode or install Docker
3. **Database connection**: Check if database container is running
4. **File not found**: Verify SQL file path and permissions

### Getting Help

```bash
# Show export script help
./db-export.sh --help

# Show import script help
./db-import.sh --help
```

## Testing

Run the validation test script:
```bash
./test-db-scripts.sh
```

For complete documentation, see:
- [Installation Guide](../docs/INSTALL.md)
- [SSH Database Guide](../docs/SSH-DATABASE-GUIDE.md)