import { BrowserQRCodeReader, type IScannerControls } from '@zxing/browser';
import { Camera, QrCode } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Input } from '../components/ui';
import { isWebNfcSupported, nfcUnsupportedMessage } from '../utils/nfc';

export function ScannerPage() {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const controlsRef = useRef<IScannerControls | null>(null);
  const [manual, setManual] = useState('');
  const [cameraState, setCameraState] = useState('Camera inactive');
  const navigate = useNavigate();

  useEffect(() => () => controlsRef.current?.stop(), []);

  async function startCamera() {
    if (!videoRef.current) return;
    try {
      setCameraState('Camera active');
      const reader = new BrowserQRCodeReader();
      controlsRef.current = await reader.decodeFromVideoDevice(undefined, videoRef.current, (result) => {
        const text = result?.getText();
        if (text) openEmergency(text);
      });
    } catch {
      setCameraState('Camera indisponible sur cet appareil');
    }
  }

  function openEmergency(value: string) {
    const token = extractToken(value);
    if (token) navigate(`/urgence/${encodeURIComponent(token)}`);
  }

  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Scanner</p><h1>Scanner un QR d'urgence</h1><p>Le contenu scanne reste dans le navigateur et sert uniquement a ouvrir la fiche urgence.</p></div>
      <div className="scanner-grid">
        <Card>
          <video ref={videoRef} className="scanner-video" muted playsInline aria-label="Apercu camera" />
          <p className="muted">{cameraState}</p>
          <Button type="button" onClick={startCamera}><Camera size={18} /> Activer la camera</Button>
        </Card>
        <Card>
          <QrCode />
          <h2>Demonstration manuelle</h2>
          <label>Lien ou token d'urgence<Input value={manual} onChange={(event) => setManual(event.target.value)} placeholder="http://localhost:5173/urgence/token" /></label>
          <Button type="button" onClick={() => openEmergency(manual)}>Ouvrir la fiche</Button>
          <p className="muted">{isWebNfcSupported() ? 'Web NFC semble disponible sur ce navigateur.' : nfcUnsupportedMessage}</p>
        </Card>
      </div>
    </div>
  );
}

function extractToken(value: string) {
  const trimmed = value.trim();
  if (!trimmed) return '';
  if (trimmed.includes('/urgence/')) return trimmed.split('/urgence/').pop() ?? '';
  if (trimmed.includes('/emergency/')) return trimmed.split('/emergency/').pop() ?? '';
  return trimmed;
}