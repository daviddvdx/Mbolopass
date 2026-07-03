import logoUrl from '../assets/logo.png';

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <div className={compact ? 'logo-mark compact' : 'logo-mark'} aria-label="MboloPass">
      <img src={logoUrl} alt="MboloPass" />
    </div>
  );
}