# SSH Database Management Guide

This guide explains how to manage Revix database import/export operations via SSH connections, including using tools like PuTTY.

## Prerequisites

- SSH access to your Revix server
- Revix installed and running
- Basic command line knowledge

## SSH Connection Setup

### Using PuTTY (Windows)

1. **Configure Connection**:
   - Host: Your server IP address
   - Port: 22 (or your custom SSH port)
   - Connection type: SSH

2. **Authentication**:
   - Use SSH keys (recommended) or password authentication
   - For SSH keys: Go to Connection > SSH > Auth and browse to your private key file

3. **Save Session**:
   - Enter a name under "Saved Sessions" and click "Save"

### Using Terminal (Linux/Mac)

```bash
# Connect to your server
ssh username@your-server-ip

# Or with SSH key
ssh -i /path/to/your/key username@your-server-ip
```

## Database Export Operations

### Basic Export

```bash
# Navigate to Revix directory
cd /path/to/Revix

# Export current database
./scripts/db-export.sh

# Export with custom filename
./scripts/db-export.sh -f "revix_backup_$(date +%Y%m%d_%H%M%S).sql"
```

### Compressed Export (Recommended for SSH)

```bash
# Export and compress (saves bandwidth)
./scripts/db-export.sh -c -f "backup_$(date +%Y%m%d).sql"

# This creates backup_20240905.sql.gz
```

### Export Options

```bash
# Schema only (structure without data)
./scripts/db-export.sh --schema-only -f schema.sql

# Data only (no structure)
./scripts/db-export.sh --data-only -f data.sql

# Custom output directory
./scripts/db-export.sh -d /home/user/backups -f backup.sql

# Show help for all options
./scripts/db-export.sh --help
```

## Database Import Operations

### Basic Import

```bash
# Import a SQL file (with confirmation prompt)
./scripts/db-import.sh backup.sql

# Import compressed file
./scripts/db-import.sh backup.sql.gz
```

### Import Options

```bash
# Clean import (removes all existing data first)
./scripts/db-import.sh --clean backup.sql

# Skip confirmation prompt (use with caution)
./scripts/db-import.sh --confirm backup.sql

# Verbose output
./scripts/db-import.sh -v backup.sql

# Show help for all options
./scripts/db-import.sh --help
```

## File Transfer via SSH

### Download Backup Files (Pull from Server)

```bash
# Using SCP to download compressed backup
scp username@server:/path/to/Revix/backups/backup.sql.gz ./local-backup.sql.gz

# Using rsync for better resume capability
rsync -avz username@server:/path/to/Revix/backups/ ./local-backups/
```

### Upload Backup Files (Push to Server)

```bash
# Upload SQL file to server
scp ./local-backup.sql username@server:/path/to/Revix/backups/

# Upload compressed file
scp ./local-backup.sql.gz username@server:/path/to/Revix/backups/
```

### Using SFTP (Interactive)

```bash
# Connect with SFTP
sftp username@server

# Navigate to backup directory
cd /path/to/Revix/backups

# Download file
get backup.sql.gz

# Upload file
put local-backup.sql

# Exit SFTP
quit
```

## Automated Backup Scripts

### Daily Backup via Cron

Create a backup script for automated exports:

```bash
# Create backup script
cat > /home/user/backup-revix.sh << 'EOF'
#!/bin/bash
cd /path/to/Revix
./scripts/db-export.sh -c -f "daily_backup_$(date +%Y%m%d).sql"

# Keep only last 7 days of backups
find backups/ -name "daily_backup_*.sql.gz" -mtime +7 -delete
EOF

chmod +x /home/user/backup-revix.sh

# Add to crontab (daily at 2 AM)
echo "0 2 * * * /home/user/backup-revix.sh" | crontab -
```

### Weekly Full Backup

```bash
# Create weekly backup script
cat > /home/user/weekly-backup.sh << 'EOF'
#!/bin/bash
cd /path/to/Revix
./scripts/db-export.sh -c -f "weekly_backup_$(date +%Y_week_%U).sql"

# Keep only last 4 weeks
find backups/ -name "weekly_backup_*.sql.gz" -mtime +28 -delete
EOF

chmod +x /home/user/weekly-backup.sh

# Add to crontab (Sundays at 3 AM)
echo "0 3 * * 0 /home/user/weekly-backup.sh" | crontab -
```

## Troubleshooting SSH Operations

### Permission Issues

```bash
# If you get permission denied
sudo chown -R $USER:$USER /path/to/Revix
chmod +x /path/to/Revix/scripts/*.sh
```

### Docker Not Available

```bash
# Use direct PostgreSQL connection
export REVIX_DB_HOST=localhost
export REVIX_DB_PORT=5432
export REVIX_DB_NAME=revix
export REVIX_DB_USER=revix
export REVIX_DB_PASS=your_password

./scripts/db-export.sh --direct
./scripts/db-import.sh --direct backup.sql
```

### Large File Transfers

```bash
# For large backups, use compression and check progress
./scripts/db-export.sh -c -f large_backup.sql

# Monitor transfer progress with rsync
rsync -avz --progress username@server:/path/to/backup.sql.gz ./
```

### Connection Timeouts

```bash
# For long operations, use screen or tmux
screen -S revix-backup
./scripts/db-export.sh -c -f huge_backup.sql

# Detach with Ctrl+A, D
# Reattach with: screen -r revix-backup
```

## Security Best Practices

### SSH Security

1. **Use SSH Keys**: Always prefer SSH key authentication over passwords
2. **Limit Access**: Use specific user accounts for database operations
3. **Firewall Rules**: Restrict SSH access to specific IP addresses
4. **Regular Updates**: Keep SSH server and client software updated

### Database Security

1. **Secure Backups**: Set restrictive permissions on backup files
   ```bash
   chmod 600 /path/to/backup.sql
   ```

2. **Encrypt Transfers**: Always use SFTP/SCP, never plain FTP
3. **Clean Up**: Remove old backup files regularly
4. **Monitor Access**: Log and monitor database access

### File Handling

```bash
# Secure backup file permissions
chmod 600 backup.sql
chown user:user backup.sql

# Secure backup directory
chmod 700 /path/to/Revix/backups/

# Remove backup after transfer
rm backup.sql.gz
```

## Common Workflows

### Complete Backup Workflow

```bash
# 1. Connect to server
ssh user@server

# 2. Navigate to Revix
cd /path/to/Revix

# 3. Create backup
./scripts/db-export.sh -c -f "backup_$(date +%Y%m%d_%H%M%S).sql"

# 4. Download backup (from local machine)
scp user@server:/path/to/Revix/backups/backup_*.sql.gz ./

# 5. Clean up remote backup
ssh user@server "rm /path/to/Revix/backups/backup_*.sql.gz"
```

### Complete Restore Workflow

```bash
# 1. Upload backup to server (from local machine)
scp ./backup.sql.gz user@server:/tmp/

# 2. Connect to server
ssh user@server

# 3. Move backup to Revix directory
mv /tmp/backup.sql.gz /path/to/Revix/backups/

# 4. Navigate to Revix
cd /path/to/Revix

# 5. Import backup
./scripts/db-import.sh backups/backup.sql.gz

# 6. Clean up
rm backups/backup.sql.gz
```

## Support

For issues with SSH database operations:

1. Check the script logs and error messages
2. Verify SSH connection and permissions
3. Ensure database is running and accessible
4. Check available disk space for backup operations
5. Review firewall and network settings

For more help, refer to the main [Installation Guide](INSTALL.md) or create an issue on the GitHub repository.