/* eslint-disable react-refresh/only-export-components */
import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { setApiAuthToken, UNAUTHORIZED_EVENT } from '../api/client';
import type { Role, User } from '../types';
import { clearLegacyAuthStorage } from './legacyAuthStorage';

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

type AuthContextType = {
  status: AuthStatus;
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isPatient: boolean;
  sessionMessage: string | null;
  hasRole: (role: Role | `ROLE_${Role}`) => boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  setSession: (token: string, user: User) => void;
  clearSession: () => void;
  consumeSessionMessage: () => void;
  setUser: (user: User | null) => void;
};

const VALID_ROLES: Role[] = ['PATIENT', 'PROFESSIONAL', 'ADMIN'];
const AuthContext = createContext<AuthContextType | undefined>(undefined);

function authority(role: Role | `ROLE_${Role}`): `ROLE_${Role}` {
  if (role === 'ADMIN' || role === 'ROLE_ADMIN') return 'ROLE_ADMIN';
  if (role === 'PROFESSIONAL' || role === 'ROLE_PROFESSIONAL') return 'ROLE_PROFESSIONAL';
  return 'ROLE_PATIENT';
}

function normalizeUser(user: User): User | null {
  if (!VALID_ROLES.includes(user.role)) return null;
  const roles = Array.from(new Set([...(user.roles ?? []), authority(user.role)]));
  const allowedAuthorities = roles.filter((role): role is `ROLE_${Role}` =>
    role === 'ROLE_PATIENT' || role === 'ROLE_PROFESSIONAL' || role === 'ROLE_ADMIN',
  );
  if (!allowedAuthorities.includes(authority(user.role))) return null;
  return { ...user, roles: allowedAuthorities };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [token, setToken] = useState<string | null>(null);
  const [user, setUserState] = useState<User | null>(null);
  const [sessionMessage, setSessionMessage] = useState<string | null>(null);

  const clearSessionState = useCallback(() => {
    setApiAuthToken(null);
    setToken(null);
    setUserState(null);
    setStatus('unauthenticated');
    queryClient.clear();
    clearLegacyAuthStorage();
  }, [queryClient]);

  const login = useCallback((nextToken: string, nextUser: User) => {
    const normalizedUser = normalizeUser(nextUser);
    queryClient.clear();
    clearLegacyAuthStorage();
    if (!normalizedUser) {
      setApiAuthToken(null);
      setToken(null);
      setUserState(null);
      setStatus('unauthenticated');
      setSessionMessage('Role utilisateur invalide. Contactez un administrateur.');
      return;
    }
    setApiAuthToken(nextToken);
    setToken(nextToken);
    setUserState(normalizedUser);
    setStatus('authenticated');
    setSessionMessage(null);
  }, [queryClient]);

  const setUser = useCallback((nextUser: User | null) => {
    setUserState(nextUser ? normalizeUser(nextUser) : null);
  }, []);

  const logout = useCallback(() => {
    clearSessionState();
  }, [clearSessionState]);

  const expireSession = useCallback(() => {
    clearSessionState();
    setSessionMessage('Votre session a expire. Veuillez vous reconnecter.');
  }, [clearSessionState]);

  const consumeSessionMessage = useCallback(() => setSessionMessage(null), []);

  useEffect(() => {
    clearLegacyAuthStorage();
    setStatus('unauthenticated');
  }, []);

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, expireSession);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, expireSession);
  }, [expireSession]);

  const hasRole = useCallback((role: Role | `ROLE_${Role}`) => {
    const expected = authority(role);
    return user?.roles.includes(expected) ?? false;
  }, [user]);

  const value = useMemo<AuthContextType>(() => ({
    status,
    token,
    user,
    isAuthenticated: status === 'authenticated' && Boolean(token && user),
    isAdmin: hasRole('ADMIN'),
    isPatient: hasRole('PATIENT'),
    sessionMessage,
    hasRole,
    login,
    logout,
    setSession: login,
    clearSession: logout,
    consumeSessionMessage,
    setUser,
  }), [status, token, user, hasRole, sessionMessage, login, logout, consumeSessionMessage, setUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) throw new Error('useAuth doit etre utilise dans AuthProvider');
  return context;
}
