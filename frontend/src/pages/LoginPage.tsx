import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { LogIn } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { login, me } from '../api/auth.api';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Logo } from '../components/Logo';
import { Button, Card, Input } from '../components/ui';
import type { User } from '../types';

const loginSchema = z.object({
  email: z.string({ required_error: 'L adresse e-mail est obligatoire.' }).trim().min(1, 'L adresse e-mail est obligatoire.').email('Veuillez saisir une adresse e-mail valide.'),
  password: z.string({ required_error: 'Le mot de passe est obligatoire.' }).trim().min(1, 'Le mot de passe est obligatoire.')
});

type FormValues = z.infer<typeof loginSchema>;

function destinationFor(user: User, requestedPath?: string) {
  if (user.roles.includes('ROLE_HEALTH_ADMIN')) {
    return requestedPath?.startsWith('/admin') ? requestedPath : '/admin';
  }
  if (user.roles.includes('ROLE_HEALTH_PROFESSIONAL')) {
    if (user.professionalProfile?.verificationStatus === 'PENDING') return '/professional/pending-verification';
    if (user.professionalProfile?.verificationStatus !== 'APPROVED') return '/professional/access-restricted';
    return requestedPath?.startsWith('/professional') ? requestedPath : '/professional';
  }
  if (user.roles.includes('ROLE_PATIENT')) {
    return requestedPath && !requestedPath.startsWith('/admin') && requestedPath !== '/non-autorise'
      ? requestedPath
      : '/tableau-de-bord';
  }
  return '/non-autorise';
}

export function LoginPage() {
  const { setSession, sessionMessage, consumeSessionMessage } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const form = useForm<FormValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' }, mode: 'onSubmit' });
  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const response = await login(values);
      const validatedUser = await me(response.accessToken);
      return { accessToken: response.accessToken, user: validatedUser };
    },
    onSuccess: ({ accessToken, user }) => {
      setSession(accessToken, user);
      const requestedPath = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname;
      const next = destinationFor(user, requestedPath);
      navigate(next, { replace: true });
    }
  });

  useEffect(() => () => consumeSessionMessage(), [consumeSessionMessage]);

  function submit(values: FormValues) {
    mutation.mutate({ email: values.email.trim(), password: values.password.trim() });
  }

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Logo />
        <h1>Connexion</h1>
        <p>Accedez a votre passeport de sante numerique.</p>
        <form noValidate onSubmit={form.handleSubmit(submit)}>
          <label htmlFor="login-email">Email</label>
          <Input id="login-email" type="email" autoComplete="email" aria-invalid={Boolean(form.formState.errors.email)} {...form.register('email')} />
          {form.formState.errors.email ? <p className="field-error">{form.formState.errors.email.message}</p> : null}

          <label htmlFor="login-password">Mot de passe</label>
          <Input id="login-password" type="password" autoComplete="current-password" aria-invalid={Boolean(form.formState.errors.password)} {...form.register('password')} />
          {form.formState.errors.password ? <p className="field-error">{form.formState.errors.password.message}</p> : null}

          {sessionMessage ? <p className="error">{sessionMessage}</p> : null}
          {mutation.error ? <p className="error">{mutation.error instanceof ApiError ? mutation.error.message : 'Connexion impossible'}</p> : null}
          <Button type="submit" disabled={mutation.isPending}><LogIn size={18} /> {mutation.isPending ? 'Connexion...' : 'Se connecter'}</Button>
        </form>
        <Link to="/inscription">Creer un compte MboloPass</Link>
      </Card>
    </main>
  );
}
