import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  CheckCircle2,
  CreditCard,
  LockKeyhole,
  PlusCircle,
  QrCode,
  RefreshCw,
  ShieldCheck,
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import {
  type CSSProperties,
  type ReactNode,
  useEffect,
  useMemo,
  useState,
} from 'react';

import cardImage from '../assets/card/card1.png';
import { generateMyQrToken, getMyCard } from '../api/card.api';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, LoadingState } from '../components/ui';
import type { CardInfo, QrTokenResponse } from '../types';

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

function getQrUrl(
    card: MboloCard | null | undefined,
    generatedQrUrl: string | null,
) {
  return (
      generatedQrUrl ||
      card?.emergencyUrl ||
      card?.qrUrl ||
      card?.activeQrUrl ||
      null
  );
}

function getStatusTone(status?: string): 'teal' | 'warning' {
  return status === 'ACTIVE' ? 'teal' : 'warning';
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
  isMobile,
  statusLabel,
  statusTone,
  children,
}: {
  isMobile: boolean;
  statusLabel: string;
  statusTone: 'teal' | 'warning';
  children?: ReactNode;
}) {
  /*
    Exemple futur :

    <span
      style={{
        ...styles.cardTextOverlay,
        left: '12%',
        top: '61%',
      }}
    >
      {card?.fullName}
    </span>
  */

  return (
      <section style={styles.cardShowcase}>
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

            <div style={styles.cardDataLayer}>{children}</div>
          </div>
        </div>

        <p style={styles.cardShowcaseFooter}>
          Aperçu de votre carte personnelle, sécurisée et prête à être utilisée.
        </p>
      </section>
  );
}

export function CardPage() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const isMobile = useIsMobile();

  const [generatedQrUrl, setGeneratedQrUrl] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

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

  const generateQrMutation = useMutation<QrTokenResponse, Error>({
    mutationFn: async () => {
      if (!isAuthenticated) {
        throw new Error('Votre session a expiré. Veuillez vous reconnecter.');
      }

      return await generateMyQrToken();
    },

    onSuccess: async (response) => {
      const newQrUrl = response.emergencyUrl || null;

      setGeneratedQrUrl(newQrUrl);
      setSuccessMessage(
          newQrUrl
              ? 'Votre QR Code d’urgence est maintenant actif.'
              : 'Votre carte MboloPass a été mise à jour.',
      );

      await queryClient.invalidateQueries({
        queryKey: ['health-card', 'me'],
      });
    },

    onError: () => {
      setSuccessMessage(null);
    },
  });

  const card = cardQuery.data;
  const hasCard = Boolean(card);

  const qrUrl = useMemo(() => {
    return getQrUrl(card, generatedQrUrl);
  }, [card, generatedQrUrl]);

  const qrIsActive = Boolean(qrUrl) && card?.qrStatus === 'ACTIVE';

  function handleCreateCard() {
    setSuccessMessage(null);
    generateQrMutation.mutate();
  }

  function handleRegenerateQr() {
    setSuccessMessage(null);

    const confirmed = window.confirm(
        'La régénération désactivera immédiatement votre ancien QR Code. Continuer ?',
    );

    if (!confirmed) {
      return;
    }

    generateQrMutation.mutate();
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

              {generateQrMutation.isError && (
                  <div style={styles.inlineError}>
                    <AlertCircle size={18} />
                    <span>{getErrorMessage(generateQrMutation.error)}</span>
                  </div>
              )}

              <div style={styles.actions}>
                <Button
                    type="button"
                    onClick={handleCreateCard}
                    disabled={generateQrMutation.isPending}
                >
                  {generateQrMutation.isPending ? (
                      <>
                        <RefreshCw size={18} style={styles.spin} />
                        Création en cours...
                      </>
                  ) : (
                      <>
                        <PlusCircle size={18} />
                        Créer ma carte MboloPass
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
              isMobile={isMobile}
              statusLabel={card?.qrStatus === 'ACTIVE' ? 'CARTE ACTIVE' : 'À ACTIVER'}
              statusTone={getStatusTone(card?.qrStatus)}
          />

          <section style={styles.contentPanel}>
            <div style={styles.qrHeader}>
              <div>
                <p style={styles.eyebrow}>Accès d’urgence</p>
                <h2 style={styles.panelTitle}>Mon QR Code sécurisé</h2>
              </div>

              <Badge tone={qrIsActive ? 'teal' : 'warning'}>
                {qrIsActive ? 'ACTIF' : 'À GÉNÉRER'}
              </Badge>
            </div>

            {successMessage && (
                <div style={styles.inlineSuccess}>
                  <CheckCircle2 size={18} />
                  <span>{successMessage}</span>
                </div>
            )}

            {generateQrMutation.isError && (
                <div style={styles.inlineError}>
                  <AlertCircle size={18} />
                  <span>{getErrorMessage(generateQrMutation.error)}</span>
                </div>
            )}

            {qrUrl ? (
                <div style={styles.qrZone}>
                  <div style={styles.qrWrapper}>
                    <QRCodeSVG
                        value={qrUrl}
                        size={isMobile ? 175 : 210}
                        includeMargin
                        level="H"
                    />
                  </div>

                  <div style={styles.qrLabel}>
                    <QrCode size={19} />
                    <span>QR Code d’accès d’urgence sécurisé</span>
                  </div>
                </div>
            ) : (
                <div style={styles.emptyQrState}>
                  <QrCode size={46} />
                  <h3 style={styles.emptyQrTitle}>
                    Votre QR Code n’est pas encore généré
                  </h3>
                  <p style={styles.emptyQrText}>
                    Générez-le maintenant afin de pouvoir utiliser votre carte en
                    situation d’urgence.
                  </p>
                </div>
            )}

            <div style={styles.securityNote}>
              <ShieldCheck size={21} />
              <p style={styles.securityText}>
                Ce QR Code ne contient aucune donnée médicale. Il donne accès à
                une fiche d’urgence limitée et sécurisée.
              </p>
            </div>

            <div style={styles.actions}>
              {!qrUrl ? (
                  <Button
                      type="button"
                      onClick={handleCreateCard}
                      disabled={generateQrMutation.isPending}
                  >
                    {generateQrMutation.isPending ? (
                        <>
                          <RefreshCw size={18} style={styles.spin} />
                          Génération en cours...
                        </>
                    ) : (
                        <>
                          <QrCode size={18} />
                          Générer mon QR Code
                        </>
                    )}
                  </Button>
              ) : (
                  <Button
                      type="button"
                      onClick={handleRegenerateQr}
                      disabled={generateQrMutation.isPending}
                  >
                    {generateQrMutation.isPending ? (
                        <>
                          <RefreshCw size={18} style={styles.spin} />
                          Régénération en cours...
                        </>
                    ) : (
                        <>
                          <RefreshCw size={18} />
                          Régénérer mon QR Code
                        </>
                    )}
                  </Button>
              )}
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

  cardTextOverlay: {
    position: 'absolute',
    transform: 'translate(-50%, -50%)',
    color: '#17304D',
    fontSize: 'clamp(0.72rem, 1.15vw, 1rem)',
    fontWeight: 800,
    lineHeight: 1.1,
    whiteSpace: 'nowrap',
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
    marginTop: 'auto',
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
