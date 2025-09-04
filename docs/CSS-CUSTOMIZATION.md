# CSS Customization Guide

This guide explains how to customize the appearance of your Revix installation by editing CSS files directly via SSH terminal or file system.

## Overview

Revix supports custom CSS styling through an external `custom.css` file that you can edit without rebuilding the application. Your custom styles will override the default theme.

## Location

Custom CSS file: `config/css/custom.css`

This file is created automatically when you first start Revix. If it doesn't exist, you can create it manually.

## How to Edit

### Via SSH Terminal

1. Connect to your server via SSH
2. Navigate to your Revix installation directory
3. Edit the custom CSS file:

```bash
# Using nano editor
nano config/css/custom.css

# Using vim editor  
vim config/css/custom.css

# Using any other text editor
```

### Via File Manager/FTP

1. Navigate to your Revix installation directory
2. Open `config/css/custom.css` in any text editor
3. Make your changes and save

## Applying Changes

After editing the CSS file:

1. **Docker**: Restart the Revix container
   ```bash
   cd deploy/docker
   docker compose restart revix
   ```

2. **Manual Installation**: Restart the Revix server
   ```bash
   # Stop the server (Ctrl+C if running in terminal)
   # Then restart it
   ./server/build/install/server/bin/server
   ```

## CSS Variables Reference

Revix uses CSS custom properties (variables) for easy theming. Here are the main variables you can customize:

### Colors

```css
:root {
    /* Primary colors */
    --primary-color: #2563eb;        /* Main brand color */
    --primary-dark: #1d4ed8;         /* Darker brand color */
    --primary-light: #3b82f6;        /* Lighter brand color */
    
    /* Status colors */
    --success-color: #16a34a;        /* Success/green */
    --warning-color: #f59e0b;        /* Warning/orange */
    --error-color: #dc2626;          /* Error/red */
    --info-color: #0ea5e9;           /* Info/blue */
    
    /* Background colors */
    --background-color: #f8fafc;     /* Page background */
    --surface-color: #ffffff;        /* Card/panel background */
    --surface-hover: #f1f5f9;        /* Hover state */
    
    /* Text colors */
    --text-primary: #1e293b;         /* Main text */
    --text-secondary: #64748b;       /* Secondary text */
    --text-light: #94a3b8;           /* Light text */
    
    /* Border colors */
    --border-color: #e2e8f0;         /* Default borders */
    --border-dark: #cbd5e1;          /* Darker borders */
}
```

### Typography

```css
:root {
    /* Font sizes */
    --font-size-xs: 0.75rem;
    --font-size-sm: 0.875rem;
    --font-size-base: 1rem;
    --font-size-lg: 1.125rem;
    --font-size-xl: 1.25rem;
    --font-size-2xl: 1.5rem;
    
    /* Font weights */
    --font-weight-normal: 400;
    --font-weight-medium: 500;
    --font-weight-semibold: 600;
    --font-weight-bold: 700;
}
```

### Layout

```css
:root {
    /* Spacing */
    --spacing-xs: 0.25rem;
    --spacing-sm: 0.5rem;
    --spacing-md: 1rem;
    --spacing-lg: 1.5rem;
    --spacing-xl: 2rem;
    
    /* Border radius */
    --radius-sm: 0.375rem;
    --radius-md: 0.5rem;
    --radius-lg: 0.75rem;
    
    /* Shadows */
    --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
    --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}
```

## Common Customizations

### Dark Theme

```css
:root {
    --background-color: #0f172a;
    --surface-color: #1e293b;
    --surface-hover: #334155;
    --text-primary: #f8fafc;
    --text-secondary: #cbd5e1;
    --border-color: #334155;
}
```

### Custom Brand Colors

```css
:root {
    /* Green theme */
    --primary-color: #16a34a;
    --primary-dark: #15803d;
    --primary-light: #22c55e;
}
```

### Custom Font

```css
body {
    font-family: 'Georgia', 'Times New Roman', serif;
}
```

### Larger Text

```css
:root {
    --font-size-base: 1.125rem;
    --font-size-lg: 1.25rem;
    --font-size-xl: 1.5rem;
}
```

### Custom Card Styling

```css
.card {
    border-radius: 12px;
    box-shadow: 0 8px 25px -5px rgba(0, 0, 0, 0.15);
    border: 1px solid var(--border-color);
}
```

### Custom Navigation

```css
.navbar {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.nav-link {
    color: white;
}
```

## Advanced Customizations

### Component-Specific Styling

```css
/* Vehicle cards */
.vehicle-card {
    background: linear-gradient(145deg, #f0f9ff, #e0f2fe);
}

/* Parts grid */
.parts-grid {
    gap: 2rem;
}

/* Buttons */
.btn-primary {
    background: linear-gradient(45deg, #2563eb, #3b82f6);
    border: none;
    box-shadow: 0 4px 15px rgba(37, 99, 235, 0.3);
}
```

### Responsive Customizations

```css
@media (max-width: 768px) {
    :root {
        --font-size-base: 0.875rem;
        --spacing-md: 0.75rem;
    }
}
```

## Troubleshooting

### Changes Not Appearing

1. **Clear browser cache**: Press Ctrl+F5 (or Cmd+Shift+R on Mac)
2. **Check file permissions**: Ensure the CSS file is readable
3. **Restart server**: Always restart Revix after making changes
4. **Check syntax**: Ensure your CSS is valid (no syntax errors)

### CSS Not Loading

1. **Check file exists**: Verify `config/css/custom.css` exists
2. **Check file path**: Ensure you're editing the correct file
3. **Check server logs**: Look for any errors in the server output

### Invalid CSS

1. **Use browser dev tools**: Check for CSS errors in the browser console
2. **Validate CSS**: Use online CSS validators
3. **Test incrementally**: Add changes one at a time to identify issues

## File Structure

```
your-revix-directory/
├── config/
│   └── css/
│       └── custom.css          ← Your custom CSS file
├── server/
│   └── ...
└── ...
```

## Security Notes

- The `config/css/custom.css` file is served directly to browsers
- Only put trusted CSS in this file
- Be careful with external resources (fonts, images) in your CSS
- Consider backing up your custom CSS file before making major changes

## Examples Repository

You can find more CSS customization examples in the Revix repository:
- Browse the default styles in `server/src/main/resources/static/css/style.css`
- Check the CSS variables and structure for reference
- Copy and modify existing styles as needed

## Support

If you need help with CSS customization:
1. Check the browser developer tools for CSS errors
2. Review the default CSS structure for reference
3. Create an issue on the Revix GitHub repository
4. Join the community discussions for tips and examples