# Application Icons

This directory contains application icons for the Cashier System.

## Required Icons

### 1. app-icon.png
- **Purpose**: JavaFX application icon (cross-platform)
- **Recommended Size**: 256x256 pixels
- **Format**: PNG with transparency
- **Usage**: Loaded by JavaFX for application window

### 2. app-icon.ico
- **Purpose**: Windows shortcut and executable icon
- **Recommended Sizes**: Multi-resolution (16x16, 32x32, 48x48, 256x256)
- **Format**: ICO format
- **Usage**: Desktop shortcuts, start menu, taskbar

## Current Status

✅ **app-icon.svg** - SVG vector icon provided (for reference)
❌ **app-icon.png** - Needs to be created (256x256 PNG)
❌ **app-icon.ico** - Needs to be created (multi-resolution ICO)

## How to Create Icons

### Option 1: Online Tools
1. Visit https://www.favicon.cc/ or https://www.icoconverter.com/
2. Upload the SVG file or design your own icon
3. Download in both PNG and ICO formats

### Option 2: Using Image Editing Software
1. **GIMP** (Free):
   - Open the SVG file
   - Export as PNG (256x256)
   - Use "ICO" plugin to export as ICO

2. **Photoshop**:
   - Open SVG at 256x256
   - Save as PNG
   - Use ICO format plugin

3. **Inkscape** (Free, recommended for SVG):
   - Open the SVG
   - Export PNG (256x256)
   - Use online converter for ICO

### Option 3: Command Line (Linux/Mac)
```bash
# Using ImageMagick
convert app-icon.svg -resize 256x256 app-icon.png
convert app-icon.svg -define icon:auto-resize=256,128,96,64,48,32,16 app-icon.ico
```

## Icon Design Guidelines

1. **Style**: Clean, modern, professional
2. **Colors**: Use the app's primary color (#4A82BA blue)
3. **Symbol**: Cash register, calculator, or shopping cart
4. **Readability**: Must be recognizable at small sizes (16x16)
5. **Background**: Transparent for PNG, appropriate for ICO

## Temporary Workaround

Until proper icons are created, the application will use JavaFX's default icon.
This doesn't affect functionality, only the visual appearance.

## After Adding Icons

Once you've added `app-icon.png` and `app-icon.ico`:

1. Rebuild the project:
   ```bash
   mvn clean package
   ```

2. Test the application:
   ```bash
   # On Windows
   start.bat

   # On Linux/Mac
   mvn javafx:run
   ```

3. Verify the icon appears in:
   - Application window title bar
   - Taskbar/dock
   - Desktop shortcut (if created)

## Need a Custom Icon?

If you need a custom-designed icon, you can:
1. Hire a designer on Fiverr, 99designs, or Dribbble
2. Use icon libraries like IconFinder or Flaticon
3. Create your own using tools like Canva or Figma
