# User Guides - Example Structure

This directory structure shows how to organize user guide documentation for the TestManagement Tool.

## Directory Structure

```
user-guides-example/
├── README.md                    # This file
├── DMDEDT/                      # Decision Manager & Decision Execution guides
│   ├── Decision Manager Admin Guide.pdf
│   ├── Decision Execution Admin Guide.pdf
│   ├── Installation Guide.pdf
│   └── Security Guide.pdf
├── DAN/                         # Decision Analytics guides
│   ├── Installation Guide.pdf
│   └── User Guide.pdf
└── bestPractices/              # Best practices documentation
    ├── Decision Best Practices.pdf
    └── Training Materials.pdf
```

## Setup Instructions

1. Copy your PDF/documentation files into the appropriate subdirectories
2. Do NOT commit these files to the repository (they are ignored in `.gitignore`)
3. Keep these guides locally for reference and development purposes

## Notes

- **DMDEDT**: Decision Manager (DM) and Decision Execution (DE) documentation
- **DAN**: Decision Analytics (DAN) documentation
- **bestPractices**: Training materials and best practice guides

## Sensitive Information

Never commit the following to this repository:
- API keys or credentials
- Database passwords
- Private configuration files
- Environment-specific configurations (use environment variables instead)
- Private keys or certificates
