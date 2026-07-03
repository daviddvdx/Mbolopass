const LEGACY_AUTH_KEYS = [
  'token',
  'auth',
  'user',
  'currentUser',
  'role',
  'roles',
  'profile',
  'mbolopass',
  'mbolopass-auth',
  'mbolopass-user',
  'mbolopass-token',
  'react-query',
  'tanstack-query',
  'REACT_QUERY_OFFLINE_CACHE',
];

export function clearLegacyAuthStorage() {
  for (const key of LEGACY_AUTH_KEYS) {
    window.localStorage.removeItem(key);
    window.sessionStorage.removeItem(key);
  }
}
