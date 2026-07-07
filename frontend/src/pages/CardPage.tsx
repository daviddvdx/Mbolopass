import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  CheckCircle2,
  CreditCard,
  LockKeyhole,
  QrCode,
  RefreshCw,
  ShieldCheck,
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import {
  type CSSProperties,
  useEffect,
  useState,
} from 'react';

import cardImage from '../assets/card/card1.png';
import {
  getEmergencyQr,
  getMyCard,
  refreshEmergencyQr,
} from '../api/card.api';
import { ApiError } from '../api/client';
import { fetchProfilePhoto } from '../api/profile.api';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, LoadingState } from '../components/ui';
import type { CardInfo, EmergencyQrResponse } from '../types';

type MboloCard = CardInfo & {
  emergencyUrl?: string;
  qrUrl?: string;
  activeQrUrl?: string;
};

function getErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Votre session a expire. Veuillez vous reconnecter.';
    if (error.status === 403) return 'Vous n etes pas autorise a acceder a cette carte.';
    if (error.status === 404) return 'Votre carte sante n a pas encore ete generee.';
    return error.message || 'Impossible de charger votre carte pour le moment.';
  }

  const apiError = error as {
    response?: {
      data?: {
        message?: string;
      };
    };
    message?: string;
  };

  return (
      apiError?.response?.data?.message ||
      apiError?.message ||
      'Une erreur est survenue. Veuillez reessayer.'
  );
}

function displayValue(value: string | null | undefined) {
  return value && value.trim() ? value : 'Non renseigné';
}

function displayCardNumber(value: string | null | undefined) {
  return value && value.trim() ? value : 'Initialisation en cours';
}

function formatDate(value: string | null | undefined) {
  if (!value) return 'Non renseigné';
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return 'Non renseigné';
  return date.toLocaleDateString('fr-FR');
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return 'Non renseigne';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Non renseigne';
  return date.toLocaleString('fr-FR');
}

function formatEmergencyContact(card: MboloCard | null | undefined) {
  const contact = card?.emergencyContact;
  if (!contact) return 'Non renseigné';
  return [contact.fullName, contact.phone].filter(Boolean).join(' - ') || 'Non renseigné';
}

function loadImage(src: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('Image impossible a charger'));
    image.src = src;
  });
}

function drawFitImage(ctx: CanvasRenderingContext2D, image: HTMLImageElement, x: number, y: number, width: number, height: number) {
  const scale = Math.min(width / image.naturalWidth, height / image.naturalHeight);
  const drawWidth = image.naturalWidth * scale;
  const drawHeight = image.naturalHeight * scale;
  ctx.drawImage(image, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight);
}

function drawText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, fontSize: number, color = '#17304D') {
  ctx.save();
  ctx.fillStyle = color;
  ctx.font = `800 ${fontSize}px Arial, sans-serif`;
  ctx.textBaseline = 'middle';
  let value = text;
  while (ctx.measureText(value).width > maxWidth && value.length > 4) {
    value = `${value.slice(0, -4)}...`;
  }
  ctx.fillText(value, x, y);
  ctx.restore();
}

async function drawQrFromDom(ctx: CanvasRenderingContext2D, x: number, y: number, size: number) {
  const svg = document.querySelector('.mbolo-card-data-layer svg');
  if (!(svg instanceof SVGElement)) return false;
  const serialized = new XMLSerializer().serializeToString(svg);
  const blob = new Blob([serialized], { type: 'image/svg+xml;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  try {
    const image = await loadImage(url);
    ctx.drawImage(image, x, y, size, size);
    return true;
  } finally {
    URL.revokeObjectURL(url);
  }
}

async function downloadCardImage(card: MboloCard, qrUrl: string | null, photoUrl: string | null) {
  const background = await loadImage(cardImage);
  const canvas = document.createElement('canvas');
  canvas.width = background.naturalWidth;
  canvas.height = background.naturalHeight;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas indisponible');

  const width = canvas.width;
  const height = canvas.height;
  ctx.drawImage(background, 0, 0, width, height);

  const photoX = width * 0.073;
  const photoY = height * 0.273;
  const photoW = width * 0.202;
  const photoH = photoW * 1.34;
  ctx.save();
  ctx.fillStyle = '#F8FCFD';
  ctx.strokeStyle = 'rgba(23, 48, 77, 0.18)';
  ctx.lineWidth = Math.max(2, width * 0.002);
  ctx.fillRect(photoX, photoY, photoW, photoH);
  ctx.strokeRect(photoX, photoY, photoW, photoH);
  if (photoUrl) {
    const photo = await loadImage(photoUrl);
    drawFitImage(ctx, photo, photoX, photoY, photoW, photoH);
  } else {
    drawText(ctx, 'Photo', photoX + photoW * 0.27, photoY + photoH * 0.5, photoW * 0.72, width * 0.018, '#8EA6AF');
  }
  ctx.restore();

  drawText(ctx, displayValue(card.fullName), width * 0.45, height * 0.308, width * 0.31, width * 0.025);
  drawText(ctx, displayCardNumber(card.cardNumber), width * 0.45, height * 0.416, width * 0.28, width * 0.019);
  drawText(ctx, displayValue(card.identityDocumentNumber), width * 0.45, height * 0.518, width * 0.28, width * 0.019);
  drawText(ctx, formatDate(card.birthDate), width * 0.45, height * 0.62, width * 0.2, width * 0.019);
  drawText(ctx, displayValue(card.gender), width * 0.418, height * 0.705, width * 0.11, width * 0.019);
  drawText(ctx, displayValue(card.bloodType), width * 0.628, height * 0.705, width * 0.11, width * 0.02, '#C94A4A');
  drawText(ctx, formatEmergencyContact(card), width * 0.40, height * 0.822, width * 0.37, width * 0.018);

  const qrSize = width * 0.202;
  const qrX = width * (1 - 0.068) - qrSize;
  const qrY = height * 0.323;
  ctx.save();
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(qrX, qrY, qrSize, qrSize);
  if (qrUrl) {
    const drawn = await drawQrFromDom(ctx, qrX + qrSize * 0.06, qrY + qrSize * 0.06, qrSize * 0.88);
    if (!drawn) drawText(ctx, 'QR', qrX + qrSize * 0.39, qrY + qrSize * 0.5, qrSize * 0.5, width * 0.02, '#8EA6AF');
  } else {
    drawText(ctx, 'QR', qrX + qrSize * 0.39, qrY + qrSize * 0.5, qrSize * 0.5, width * 0.02, '#8EA6AF');
  }
  ctx.restore();

  const link = document.createElement('a');
  link.href = canvas.toDataURL('image/png');
  link.download = `${displayCardNumber(card.cardNumber).replace(/[^a-zA-Z0-9-]/g, '-')}-mbolopass.png`;
  link.click();
}

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(() => window.innerWidth <= 860);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 860);
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return isMobile;
}

function HealthCardPreview({
  card,
  qrUrl,
  photoUrl,
  isMobile,
  statusLabel,
  statusTone,
}: {
  card: MboloCard | null | undefined;
  qrUrl: string | null;
  photoUrl: string | null;
  isMobile: boolean;
  statusLabel: string;
  statusTone: 'teal' | 'warning';
}) {
  return (
      <section className="mbolo-card-print-area" style={styles.cardShowcase}>
        <div style={styles.cardShowcaseHeader}>
          <div>
            <p style={styles.cardShowcaseEyebrow}>Passeport santé numérique</p>
            <h2 style={styles.cardShowcaseTitle}>Votre carte MboloPass</h2>
          </div>

          <Badge tone={statusTone}>{statusLabel}</Badge>
        </div>

        <div style={styles.cardStage}>
          <div style={styles.cardAmbientGlow} />

          <div
              className="mbolo-card-shell"
              style={{
                ...styles.cardShell,
                transform: isMobile
                    ? 'none'
                    : 'perspective(1200px) rotateX(3deg) rotateY(-4deg)',
              }}
          >
            <img
                src={cardImage}
                alt="Aperçu de la carte MboloPass"
                style={styles.cardArtwork}
            />

            <div className="mbolo-card-data-layer" style={styles.cardDataLayer}>
              <div style={styles.cardPhotoFrame}>
                {photoUrl ? (
                    <img src={photoUrl} alt="Photo de profil" style={styles.cardPhoto} />
                ) : (
                    <span style={styles.cardPhotoPlaceholder}>Photo</span>
                )}
              </div>

              <span style={{ ...styles.cardTextOverlay, ...styles.cardNameOverlay }}>
                {displayValue(card?.fullName)}
              </span>

              <span style={{ ...styles.cardTextOverlay, ...styles.cardNumberOverlay }}>
                {displayCardNumber(card?.cardNumber)}
              </span>

              <span style={{ ...styles.cardTextOverlay, ...styles.cardIdentityOverlay }}>
                {displayValue(card?.identityDocumentNumber)}
              </span>

              <span style={{ ...styles.cardTextOverlay, ...styles.cardBirthDateOverlay }}>
                {formatDate(card?.birthDate)}
              </span>

              <span style={{ ...styles.cardTextOverlay, ...styles.cardGenderOverlay }}>
                {displayValue(card?.gender)}
              </span>

              <span style={{ ...styles.cardTextOverlay, ...styles.cardBloodTypeOverlay }}>
                {displayValue(card?.bloodType)}
              </span>

              <span style={{ ...styles.cardTextOverlay, ...styles.cardEmergencyContactOverlay }}>
                {formatEmergencyContact(card)}
              </span>

              <div style={styles.cardQrOverlay}>
                {qrUrl ? (
                    <QRCodeSVG value={qrUrl} size={160} level="H" includeMargin style={styles.cardQrArtwork} />
                ) : (
                    <span style={styles.cardQrPlaceholder}>QR</span>
                )}
              </div>
            </div>
          </div>
        </div>

        <p style={styles.cardShowcaseFooter}>
          Aperçu de votre carte personnelle, sécurisée et prête à être utilisée.
        </p>
      </section>
  );
}

export function CardPage() {
  const { isAuthenticated, token } = useAuth();
  const queryClient = useQueryClient();
  const isMobile = useIsMobile();

  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);

  const cardQuery = useQuery<MboloCard | null, Error>({
    queryKey: ['health-card', 'me'],
    enabled: isAuthenticated,

    queryFn: async () => {
      try {
        const response = await getMyCard();

        return (response ?? null) as MboloCard | null;
      } catch (error) {
        const status = error instanceof ApiError ? error.status : undefined;

        if (status === 404) {
          return null;
        }

        throw error;
      }
    },
  });

  const emergencyQrQuery = useQuery<EmergencyQrResponse, Error>({
    queryKey: ['emergency-qr', 'me'],
    queryFn: () => getEmergencyQr(token),
    enabled: isAuthenticated && Boolean(token),
    staleTime: 60_000,
  });

  const refreshEmergencyQrMutation = useMutation<EmergencyQrResponse, Error>({
    mutationFn: async () => {
      if (!isAuthenticated) {
        throw new Error('Votre session a expire. Veuillez vous reconnecter.');
      }

      return refreshEmergencyQr(token);
    },

    onSuccess: async () => {
      setSuccessMessage('Votre QR d urgence a ete mis a jour. Imprimez une nouvelle carte pour utiliser ces nouvelles informations hors ligne.');
      await queryClient.invalidateQueries({ queryKey: ['emergency-qr', 'me'] });
      await queryClient.invalidateQueries({ queryKey: ['health-card', 'me'] });
    },

    onError: () => {
      setSuccessMessage(null);
    },
  });
  const card = cardQuery.data;
  const hasCard = Boolean(card);

  const permanentQr = emergencyQrQuery.data;
  const printableQrValue = emergencyQrQuery.data?.payload ?? null;

  useEffect(() => {
    let revoked = false;
    if (!token || !card?.hasProfilePhoto) {
      setPhotoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return null;
      });
      return undefined;
    }

    fetchProfilePhoto(token).then((blob) => {
      if (revoked) return;
      setPhotoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return blob ? URL.createObjectURL(blob) : null;
      });
    });

    return () => {
      revoked = true;
      setPhotoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return null;
      });
    };
  }, [card?.hasProfilePhoto, token]);

  function handleRefreshEmergencyQr() {
    setSuccessMessage(null);
    const confirmed = window.confirm(
        'Votre nouveau QR contiendra vos dernieres informations de sante. Les anciennes cartes imprimees continueront d afficher les anciennes donnees hors connexion. Imprimez une nouvelle carte apres cette mise a jour.',
    );
    if (confirmed) refreshEmergencyQrMutation.mutate();
  }
  function handlePrintCard() {
    window.print();
  }

  async function handleDownloadCardImage() {
    if (!card) return;
    try {
      await downloadCardImage(card, printableQrValue, photoUrl);
    } catch {
      window.alert('Impossible de telecharger l image de la carte pour le moment.');
    }
  }

  if (cardQuery.isLoading) {
    return (
        <div style={styles.page}>
          <LoadingState />
        </div>
    );
  }

  if (!isAuthenticated) {
    return (
        <div style={styles.page}>
          <section style={styles.centerPanel}>
            <LockKeyhole size={38} />
            <h2 style={styles.panelTitle}>Session expirée</h2>
            <p style={styles.mutedText}>
              Veuillez vous reconnecter pour consulter votre carte MboloPass.
            </p>
          </section>
        </div>
    );
  }

  if (cardQuery.isError) {
    return (
        <div style={styles.page}>
          <div style={styles.heading}>
            <p style={styles.eyebrow}>Carte MboloPass</p>
            <h1 style={styles.title}>Mon Passeport Santé</h1>
          </div>

          <section style={styles.errorPanel}>
            <AlertCircle size={34} />

            <div style={styles.errorText}>
              <h2 style={styles.panelTitle}>Impossible de charger votre carte</h2>
              <p style={styles.mutedText}>{getErrorMessage(cardQuery.error)}</p>
            </div>

            <Button
                type="button"
                onClick={() => cardQuery.refetch()}
                disabled={cardQuery.isFetching}
            >
              <RefreshCw size={18} />
              Réessayer
            </Button>
          </section>
        </div>
    );
  }

  if (!hasCard) {
    return (
        <div style={styles.page}>
          <div style={styles.heading}>
            <p style={styles.eyebrow}>Carte MboloPass</p>
            <h1 style={styles.title}>Votre passeport santé numérique</h1>
            <p style={styles.subtitle}>
              Créez votre carte d’urgence sécurisée afin de faciliter votre prise
              en charge en cas de besoin.
            </p>
          </div>

          <section
              style={{
                ...styles.grid,
                gridTemplateColumns: isMobile
                    ? '1fr'
                    : 'minmax(0, 1.35fr) minmax(340px, 0.75fr)',
              }}
          >
            <HealthCardPreview
                card={null}
                qrUrl={null}
                photoUrl={null}
                isMobile={isMobile}
                statusLabel="À CRÉER"
                statusTone="warning"
            />

            <section style={styles.contentPanel}>
              <div style={styles.iconBox}>
                <CreditCard size={29} />
              </div>

              <h2 style={styles.panelTitle}>Créer ma carte MboloPass</h2>

              <p style={styles.mutedText}>
                Votre carte vous donne accès à un QR Code d’urgence sécurisé,
                utilisable par les secours ou le personnel médical.
              </p>

              <div style={styles.benefitsList}>
                <div style={styles.benefitItem}>
                  <ShieldCheck size={19} />
                  <span>Accès limité aux données utiles en urgence</span>
                </div>

                <div style={styles.benefitItem}>
                  <QrCode size={19} />
                  <span>QR Code unique et révocable à tout moment</span>
                </div>

                <div style={styles.benefitItem}>
                  <LockKeyhole size={19} />
                  <span>Aucune donnée médicale stockée dans le QR Code</span>
                </div>
              </div>

              <div style={styles.actions}>
                <Button
                    type="button"
                    onClick={() => {
                      void cardQuery.refetch();
                      void emergencyQrQuery.refetch();
                    }}
                    disabled={cardQuery.isFetching || emergencyQrQuery.isFetching}
                >
                  {cardQuery.isFetching || emergencyQrQuery.isFetching ? (
                      <>
                        <RefreshCw size={18} style={styles.spin} />
                        Creation en cours...
                      </>
                  ) : (
                      <>
                        <CreditCard size={18} />
                        Creer ma carte MboloPass
                      </>
                  )}
                </Button>
              </div>
            </section>
          </section>
        </div>
    );
  }

  return (
      <div style={styles.page}>
        <div style={styles.heading}>
          <p style={styles.eyebrow}>Carte MboloPass</p>
          <h1 style={styles.title}>Ma carte d’urgence</h1>
          <p style={styles.subtitle}>
            Présentez ce QR Code aux secours ou au personnel médical en cas
            d’urgence.
          </p>
        </div>

        <section
            style={{
              ...styles.grid,
              gridTemplateColumns: isMobile
                  ? '1fr'
                  : 'minmax(0, 1.35fr) minmax(340px, 0.75fr)',
            }}
        >
          <HealthCardPreview
              card={card}
              qrUrl={printableQrValue}
              photoUrl={photoUrl}
              isMobile={isMobile}
              statusLabel={printableQrValue ? 'QR HORS LIGNE PRET' : 'QR A CREER'}
              statusTone={printableQrValue ? 'teal' : 'warning'}
          />

          <section style={styles.contentPanel}>
            <div style={styles.qrHeader}>
              <div>
                <p style={styles.eyebrow}>QR d'urgence permanent</p>
                <h2 style={styles.panelTitle}>Carte imprimable</h2>
              </div>

              <Badge tone={Boolean(permanentQr?.payload) ? 'teal' : 'warning'}>
                {Boolean(permanentQr?.payload) ? 'ACTIF' : 'DESACTIVE'}
              </Badge>
            </div>

            <p style={styles.mutedText}>
              Ce QR permet d'afficher vos informations vitales en cas d'urgence.
              Il ne donne pas acces a votre dossier medical complet.
            </p>

            {emergencyQrQuery.isLoading ? (
                <LoadingState />
            ) : emergencyQrQuery.isError ? (
                <div style={styles.inlineError}>
                  <AlertCircle size={18} />
                  <span>{getErrorMessage(emergencyQrQuery.error)}</span>
                </div>
            ) : printableQrValue ? (
                <div style={styles.qrZone}>
                  <div style={styles.qrWrapper}>
                    <QRCodeSVG
                        value={printableQrValue}
                        size={isMobile ? 190 : 260}
                        includeMargin
                        level="M"
                    />
                  </div>

                  <div style={styles.qrLabel}>
                    <QrCode size={19} />
                    <span>QR permanent - informations limitees - dossier complet protege</span>
                  </div>
                </div>
            ) : (
                <div style={styles.emptyQrState}>
                  <QrCode size={46} />
                  <h3 style={styles.emptyQrTitle}>QR d'urgence desactive</h3>
                  <p style={styles.emptyQrText}>
                    Vous pouvez le regenerer pour rendre votre carte imprimable
                    de nouveau utilisable.
                  </p>
                </div>
            )}

            <div style={styles.cardStats}>
              <span>Creation<strong>{formatDateTime(permanentQr?.generatedAt)}</strong></span>
              <span>Version<strong>{permanentQr?.version ?? '-'}</strong></span>
            </div>


            {successMessage && (
                <div style={styles.inlineSuccess}>
                  <CheckCircle2 size={18} />
                  <span>{successMessage}</span>
                </div>
            )}

            {refreshEmergencyQrMutation.isError && (
                <div style={styles.inlineError}>
                  <AlertCircle size={18} />
                  <span>{getErrorMessage(refreshEmergencyQrMutation.error)}</span>
                </div>
            )}

            <div style={styles.actions}>
              <Button
                  type="button"
                  onClick={handleRefreshEmergencyQr}
                  disabled={refreshEmergencyQrMutation.isPending}
              >
                {refreshEmergencyQrMutation.isPending ? <RefreshCw size={18} style={styles.spin} /> : <RefreshCw size={18} />}
                Mettre a jour mon QR
              </Button><Button
                  type="button"
                  className="ghost"
                  onClick={handlePrintCard}
              >
                <CreditCard size={18} />
                Imprimer la carte
              </Button>
              <Button
                  type="button"
                  className="ghost"
                  onClick={handleDownloadCardImage}
              >
                <CreditCard size={18} />
                Télécharger l'image
              </Button>
            </div>
          </section>
        </section>
      </div>
  );
}

const styles: Record<string, CSSProperties> = {
  page: {
    width: '100%',
    maxWidth: 1240,
    margin: '0 auto',
    padding: '28px 20px 42px',
  },

  heading: {
    marginBottom: 26,
  },

  eyebrow: {
    margin: 0,
    marginBottom: 8,
    fontSize: 12,
    fontWeight: 800,
    letterSpacing: '0.11em',
    textTransform: 'uppercase',
    color: '#14b8a6',
  },

  title: {
    margin: 0,
    fontSize: 'clamp(1.9rem, 3vw, 2.7rem)',
    lineHeight: 1.12,
  },

  subtitle: {
    maxWidth: 720,
    marginTop: 12,
    marginBottom: 0,
    lineHeight: 1.65,
    color: '#94a3b8',
  },

  grid: {
    display: 'grid',
    gap: 24,
    alignItems: 'stretch',
  },

  cardShowcase: {
    position: 'relative',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
    gap: 22,
    minWidth: 0,
    padding: 24,
    border: '1px solid rgba(58, 158, 205, 0.24)',
    borderRadius: 26,
    background:
        'linear-gradient(145deg, rgba(23, 48, 77, 0.96), rgba(11, 31, 58, 0.94))',
    boxShadow: '0 24px 60px rgba(15, 23, 42, 0.24)',
  },

  cardShowcaseHeader: {
    position: 'relative',
    zIndex: 2,
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 16,
  },

  cardShowcaseEyebrow: {
    margin: 0,
    marginBottom: 7,
    fontSize: 11,
    fontWeight: 800,
    letterSpacing: '0.11em',
    textTransform: 'uppercase',
    color: '#67e8f9',
  },

  cardShowcaseTitle: {
    margin: 0,
    color: '#ffffff',
    fontSize: 'clamp(1.15rem, 2vw, 1.55rem)',
    lineHeight: 1.2,
  },

  cardStage: {
    position: 'relative',
    zIndex: 1,
    display: 'grid',
    placeItems: 'center',
    minHeight: 0,
    padding: '8px 0 2px',
  },

  cardAmbientGlow: {
    position: 'absolute',
    inset: '12% 5% 4%',
    borderRadius: '50%',
    background:
        'radial-gradient(circle, rgba(51, 171, 160, 0.34), rgba(36, 113, 161, 0.18) 46%, rgba(36, 113, 161, 0) 72%)',
    filter: 'blur(28px)',
    transform: 'translateY(12px)',
  },

  cardShell: {
    position: 'relative',
    zIndex: 2,
    width: '100%',
    maxWidth: 780,
    overflow: 'hidden',
    border: '1px solid rgba(255, 255, 255, 0.24)',
    borderRadius: 24,
    background: '#ffffff',
    boxShadow:
        '0 28px 70px rgba(2, 6, 23, 0.34), 0 8px 22px rgba(51, 171, 160, 0.18)',
    transformOrigin: 'center center',
  },

  cardArtwork: {
    display: 'block',
    width: '100%',
    height: 'auto',
  },

  cardDataLayer: {
    position: 'absolute',
    inset: 0,
    pointerEvents: 'none',
  },

  cardPhotoFrame: {
    position: 'absolute',
    left: '7.3%',
    top: '27.3%',
    width: '20.2%',
    aspectRatio: '1 / 1.34',
    overflow: 'hidden',
    display: 'grid',
    placeItems: 'center',
    borderRadius: '7%',
    color: '#8EA6AF',
    background: 'linear-gradient(135deg, rgba(248, 252, 253, 0.82), rgba(203, 219, 225, 0.72))',
    border: '1px solid rgba(23, 48, 77, 0.16)',
  },

  cardPhoto: {
    maxWidth: '100%',
    maxHeight: '100%',
    width: 'auto',
    height: 'auto',
  },

  cardPhotoPlaceholder: {
    fontSize: 'clamp(0.45rem, 1.15vw, 0.82rem)',
    fontWeight: 800,
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },

  cardTextOverlay: {
    position: 'absolute',
    transform: 'translate(-50%, -50%)',
    color: '#17304D',
    fontSize: 'clamp(0.72rem, 1.15vw, 1rem)',
    fontWeight: 800,
    lineHeight: 1.1,
    whiteSpace: 'nowrap',
  },

  cardNameOverlay: {
    left: '45%',
    top: '30.8%',
    width: '31%',
    fontSize: 'clamp(0.78rem, 1.45vw, 1.25rem)',
    textAlign: 'left',
    transform: 'translate(0, -50%)',
  },

  cardNumberOverlay: {
    left: '45%',
    top: '41.6%',
    width: '28%',
    textAlign: 'left',
    transform: 'translate(0, -50%)',
  },

  cardIdentityOverlay: {
    left: '45%',
    top: '51.8%',
    width: '28%',
    textAlign: 'left',
    transform: 'translate(0, -50%)',
  },

  cardBirthDateOverlay: {
    left: '45%',
    top: '62%',
    width: '20%',
    textAlign: 'left',
    transform: 'translate(0, -50%)',
  },

  cardGenderOverlay: {
    left: '41.8%',
    top: '70.5%',
    width: '11%',
    textAlign: 'left',
    transform: 'translate(0, -50%)',
  },

  cardBloodTypeOverlay: {
    left: '62.8%',
    top: '70.5%',
    width: '11%',
    color: '#C94A4A',
    textAlign: 'left',
    transform: 'translate(0, -50%)',
  },

  cardEmergencyContactOverlay: {
    left: '40%',
    top: '82.2%',
    width: '37%',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    textAlign: 'left',
    transform: 'translate(0, -50%)',
  },

  cardQrOverlay: {
    position: 'absolute',
    right: '6.8%',
    top: '32.3%',
    width: '20.2%',
    aspectRatio: '1 / 1',
    display: 'grid',
    placeItems: 'center',
    padding: '1.3%',
    borderRadius: '6%',
    background: '#ffffff',
    boxShadow: '0 2px 8px rgba(23, 48, 77, 0.12)',
  },

  cardQrArtwork: {
    width: '100%',
    height: '100%',
  },

  cardQrPlaceholder: {
    color: '#8EA6AF',
    fontSize: 'clamp(0.58rem, 1.3vw, 0.95rem)',
    fontWeight: 900,
    letterSpacing: '0.08em',
  },

  cardShowcaseFooter: {
    position: 'relative',
    zIndex: 2,
    margin: 0,
    color: 'rgba(226, 232, 240, 0.78)',
    fontSize: 13,
    lineHeight: 1.55,
    textAlign: 'center',
  },

  contentPanel: {
    display: 'flex',
    flexDirection: 'column',
    gap: 19,
    padding: 26,
    border: '1px solid rgba(148, 163, 184, 0.18)',
    borderRadius: 22,
    background: 'rgba(15, 23, 42, 0.52)',
    boxShadow: '0 16px 42px rgba(15, 23, 42, 0.12)',
  },

  iconBox: {
    display: 'grid',
    width: 58,
    height: 58,
    placeItems: 'center',
    borderRadius: 18,
    color: '#2dd4bf',
    background: 'rgba(20, 184, 166, 0.12)',
  },

  panelTitle: {
    margin: 0,
    fontSize: '1.3rem',
    lineHeight: 1.25,
  },

  mutedText: {
    margin: 0,
    lineHeight: 1.65,
    color: '#94a3b8',
  },

  benefitsList: {
    display: 'grid',
    gap: 13,
  },

  benefitItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 11,
    fontSize: 14,
    lineHeight: 1.45,
    color: '#dbeafe',
  },

  qrHeader: {
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 16,
  },

  qrZone: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 14,
    padding: 22,
    border: '1px solid rgba(148, 163, 184, 0.22)',
    borderRadius: 18,
    background: 'rgba(255, 255, 255, 0.03)',
  },

  qrWrapper: {
    display: 'grid',
    placeItems: 'center',
    padding: 14,
    borderRadius: 16,
    background: '#ffffff',
    boxShadow: '0 12px 26px rgba(15, 23, 42, 0.14)',
  },

  qrLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: 9,
    fontSize: 14,
    fontWeight: 700,
    color: '#e2e8f0',
  },

  emptyQrState: {
    display: 'flex',
    minHeight: 260,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    padding: 22,
    textAlign: 'center',
    border: '1px dashed rgba(148, 163, 184, 0.36)',
    borderRadius: 18,
    color: '#94a3b8',
  },

  emptyQrTitle: {
    margin: 0,
    color: '#e2e8f0',
  },

  emptyQrText: {
    maxWidth: 370,
    margin: 0,
    lineHeight: 1.6,
  },

  securityNote: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: 11,
    padding: 15,
    borderRadius: 15,
    color: '#ccfbf1',
    background: 'rgba(20, 184, 166, 0.1)',
  },

  securityText: {
    margin: 0,
    fontSize: 13,
    lineHeight: 1.55,
  },

  actions: {
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 10,
    marginTop: 'auto',
  },

  cardStats: {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
    gap: 10,
  },

  divider: {
    height: 1,
    background: 'rgba(148, 163, 184, 0.22)',
  },

  inlineError: {
    display: 'flex',
    alignItems: 'center',
    gap: 9,
    padding: '12px 14px',
    borderRadius: 13,
    color: '#fecaca',
    background: 'rgba(239, 68, 68, 0.13)',
    fontSize: 14,
  },

  inlineSuccess: {
    display: 'flex',
    alignItems: 'center',
    gap: 9,
    padding: '12px 14px',
    borderRadius: 13,
    color: '#bbf7d0',
    background: 'rgba(34, 197, 94, 0.13)',
    fontSize: 14,
  },

  errorPanel: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    padding: 24,
    border: '1px solid rgba(239, 68, 68, 0.24)',
    borderRadius: 20,
    color: '#fecaca',
    background: 'rgba(239, 68, 68, 0.07)',
  },

  errorText: {
    flex: 1,
  },

  centerPanel: {
    display: 'flex',
    minHeight: 280,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    padding: 28,
    textAlign: 'center',
    border: '1px solid rgba(148, 163, 184, 0.18)',
    borderRadius: 22,
    background: 'rgba(15, 23, 42, 0.52)',
  },

  spin: {
    animation: 'mboloSpin 0.9s linear infinite',
  },
};
