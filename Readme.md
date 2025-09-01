# Revix - Self-hosted Car Maintenance Tracking

Revix is a comprehensive, self-hosted platform for tracking vehicle maintenance and managing parts. Built with Kotlin Multiplatform for maximum code sharing and modern architecture.

## 🚗 Features

- **Vehicle Management**: Track multiple vehicles with detailed information (license plate, VIN, make/model, year, fuel type)
- **Parts Catalog**: Organize parts with pricing, descriptions, URLs, and tagging system
- **Maintenance Records**: Log maintenance activities with associated parts, quantities, and notes
- **Tag System**: Categorize and filter parts using a flexible tagging system
- **Odometer Tracking**: Support for both kilometers and engine hours
- **Multi-user Support**: Secure, per-user data isolation with JWT authentication
- **REST API**: Complete RESTful API for all operations
- **Self-hosted**: Run on your own infrastructure with Docker

## 🏗️ Architecture

- **Backend**: Ktor server (Kotlin/JVM) with PostgreSQL database
- **Shared Domain**: Kotlin Multiplatform shared models and validation
- **Authentication**: JWT access/refresh tokens with BCrypt password hashing
- **Database**: PostgreSQL with Exposed ORM and Flyway migrations
- **Deployment**: Docker Compose for easy self-hosting

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- 2GB+ available RAM

### Installation
```bash
# Clone the repository
git clone https://github.com/JeffMolenaar/Revix.git
cd Revix

# Build and start with Docker Compose
./build.sh

# Verify the installation
curl http://localhost:8080/health
```

### Create Your First User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "password": "YourSecurePassword123",
    "name": "Your Name"
  }'
```

## 📊 Data Model

### Core Entities
- **User**: Authentication and ownership
- **Vehicle**: Cars, motorcycles, boats, etc. with odometer/hours tracking
- **Part**: Spare parts with pricing and supplier information
- **Tag**: Flexible categorization system
- **MaintenanceRecord**: Service records linking vehicles and parts
- **MaintenanceItem**: Individual parts used in maintenance

### Relationships
- Users own vehicles, parts, tags, and maintenance records
- Parts can be tagged with multiple tags
- Maintenance records can include multiple parts with quantities
- All data is isolated per user for security

## 🔌 API Endpoints

### Authentication
```
POST /api/v1/auth/register  # Register new user
POST /api/v1/auth/login     # Login
POST /api/v1/auth/refresh   # Refresh token
GET  /api/v1/auth/me        # Current user profile
```

### Vehicles
```
GET    /api/v1/vehicles     # List vehicles
POST   /api/v1/vehicles     # Create vehicle
GET    /api/v1/vehicles/{id} # Get vehicle
PUT    /api/v1/vehicles/{id} # Update vehicle
DELETE /api/v1/vehicles/{id} # Delete vehicle
```

### Parts & Tags
```
GET    /api/v1/parts        # List parts (with filtering)
POST   /api/v1/parts        # Create part
GET    /api/v1/tags         # List tags
POST   /api/v1/tags         # Create tag
```

### Maintenance
```
GET  /api/v1/vehicles/{id}/maintenance  # List maintenance for vehicle
POST /api/v1/vehicles/{id}/maintenance  # Create maintenance record
GET  /api/v1/maintenance/{id}           # Get maintenance record
PUT  /api/v1/maintenance/{id}           # Update maintenance record
```

## 🛠️ Development

### Prerequisites
- JDK 17+
- Gradle 8.5+
- PostgreSQL 16+

### Setup
```bash
# Clone and build
git clone https://github.com/JeffMolenaar/Revix.git
cd Revix

# Build the project
./gradlew build

# Run tests
./gradlew test

# Start development server
REVIX_JWT_SECRET=dev-secret ./gradlew server:run
```

### Project Structure
```
revix/
├── shared/           # Kotlin Multiplatform shared code
│   └── src/commonMain/kotlin/
├── server/           # Ktor backend application
│   ├── src/main/kotlin/
│   └── src/main/resources/
├── deploy/docker/    # Docker deployment files
└── docs/            # Documentation
```

## 🔒 Security

- **Authentication**: JWT tokens with configurable expiration
- **Password Security**: BCrypt hashing with salt rounds
- **Data Isolation**: Per-user data separation at database level
- **Input Validation**: Comprehensive validation for all inputs
- **CORS**: Configurable cross-origin resource sharing

## 🚀 Deployment

### Environment Variables
```bash
# Required
REVIX_JWT_SECRET=your-super-secret-jwt-key

# Optional (with defaults)
REVIX_DB_URL=jdbc:postgresql://localhost:5432/revix
REVIX_DB_USER=revix
REVIX_DB_PASS=revix
REVIX_PORT=8080
REVIX_HOST=0.0.0.0
```

### Production Deployment
1. Set a strong JWT secret
2. Use HTTPS with reverse proxy
3. Secure database with strong credentials
4. Regular backups
5. Monitor logs and performance

## 📝 Example Usage

### 1. Create a Vehicle
```bash
curl -X POST http://localhost:8080/api/v1/vehicles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "manufacturer": "Toyota",
    "model": "Camry",
    "buildYear": 2020,
    "licensePlate": "ABC123",
    "vin": "1234567890ABCDEFG",
    "odoUnit": "KM",
    "currentOdo": 45000
  }'
```

### 2. Add Parts with Tags
```bash
# Create tags
curl -X POST http://localhost:8080/api/v1/tags \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Engine Oil", "color": "#FF5722"}'

# Create part
curl -X POST http://localhost:8080/api/v1/parts \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mobil 1 5W-30 Engine Oil",
    "description": "Full synthetic motor oil",
    "priceCents": 4500,
    "currency": "USD",
    "url": "https://example.com/oil",
    "tagIds": ["tag-id-here"]
  }'
```

### 3. Log Maintenance
```bash
curl -X POST http://localhost:8080/api/v1/vehicles/{vehicle-id}/maintenance \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "happenedAt": "2024-08-26",
    "odoReading": 45000,
    "title": "Oil Change",
    "notes": "Replaced engine oil and filter",
    "items": [{
      "partId": "part-id-here",
      "quantity": 5.0,
      "unit": "L"
    }]
  }'
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🎯 Roadmap

- [ ] Web UI (Compose Multiplatform for Web)
- [ ] Android app (Compose Multiplatform)
- [ ] Desktop app (Compose Multiplatform)
- [ ] Maintenance reminders and scheduling
- [ ] Image upload for receipts and parts
- [ ] Import/export functionality
- [ ] Reporting and analytics dashboard
- [ ] Multi-language support

## 📞 Support

- 📖 [Installation Guide](docs/INSTALL.md)
- 🐛 [Issue Tracker](https://github.com/JeffMolenaar/Revix/issues)
- 💬 [Discussions](https://github.com/JeffMolenaar/Revix/discussions)

Built with ❤️ using Kotlin Multiplatform
