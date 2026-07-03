export function nfcExplanation(url?: string) {
  return `La carte NFC MboloPass contiendra exactement la meme URL opaque que le QR code${url ? `: ${url}` : '.'}`;
}

export function isWebNfcSupported() {
  return 'NDEFReader' in window;
}

export const nfcUnsupportedMessage = 'La lecture NFC depend du navigateur et de votre appareil. Utilisez le QR code comme solution universelle.';
