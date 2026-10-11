// Generates assets/images/splash.png. Run: npm run generate-splash
const sharp = require('sharp');
const path = require('path');

const WIDTH = 1284;
const HEIGHT = 2778;
const BG = '#111827';
const ACCENT = '#4F46E5';
const TEXT = '#F9FAFB';
const APP_NAME = 'Foundry';

const svg = `<svg width="${WIDTH}" height="${HEIGHT}" xmlns="http://www.w3.org/2000/svg">
  <rect width="${WIDTH}" height="${HEIGHT}" fill="${BG}"/>
  <g transform="translate(${WIDTH / 2}, ${HEIGHT / 2 - 80})">
    <rect x="-120" y="-120" width="240" height="240" rx="56" fill="${ACCENT}"/>
    <text x="0" y="0" text-anchor="middle" dominant-baseline="central" font-family="Helvetica, Arial, sans-serif" font-size="150" font-weight="700" fill="${TEXT}">${APP_NAME.charAt(0)}</text>
  </g>
  <text x="${WIDTH / 2}" y="${HEIGHT / 2 + 140}" text-anchor="middle" font-family="Helvetica, Arial, sans-serif" font-size="72" font-weight="600" fill="${TEXT}">${APP_NAME}</text>
</svg>`;

async function generate() {
    const outputPath = path.join(__dirname, '..', 'assets', 'images', 'splash.png');
    await sharp(Buffer.from(svg)).png().toFile(outputPath);
    console.log('Splash screen generated: ' + outputPath);
}

generate().catch((err) => {
    console.error('Error:', err);
    process.exit(1);
});
