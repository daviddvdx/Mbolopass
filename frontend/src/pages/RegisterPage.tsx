import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { UserPlus } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { me, registerPatient } from '../api/auth.api';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Logo } from '../components/Logo';
import { Button, Card, Input } from '../components/ui';

const schema = z.object({
  firstName: z.string().min(1, 'Prenom requis'),
  lastName: z.string().min(1, 'Nom requis'),
  email: z.string().email('Email invalide'),
  password: z.string().min(8, '8 caracteres minimum'),
  confirmPassword: z.string().min(8, 'Confirmation requise')
}).refine((value) => value.password === value.confirmPassword, { path: ['confirmPassword'], message: 'Les mots de passe ne correspondent pas' });

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { setSession } = useAuth();
  const navigate = useNavigate();
  const form = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { firstName: '', lastName: '', email: '', password: '', confirmPassword: '' } });
  const mutation = useMutation({
    mutationFn: async (payload: { firstName: string; lastName: string; email: string; password: string }) => {
      const response = await registerPatient(payload);
      const validatedUser = await me(response.accessToken);
      return { accessToken: response.accessToken, user: validatedUser };
    },
    onSuccess: ({ accessToken, user }) => {
      setSession(accessToken, user);
      navigate('/tableau-de-bord', { replace: true });
    }
  });

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Logo />
        <h1>Creer mon passeport</h1>
        <p>Le role patient est attribue automatiquement.</p>
        <form onSubmit={form.handleSubmit((values) => mutation.mutate({ firstName: values.firstName, lastName: values.lastName, email: values.email, password: values.password }))}>
          <div className="two-cols">
            <label>Prenom<Input autoComplete="given-name" {...form.register('firstName')} /></label>
            <label>Nom<Input autoComplete="family-name" {...form.register('lastName')} /></label>
          </div>
          <label>Email<Input type="email" autoComplete="email" {...form.register('email')} /></label>
          <label>Mot de passe<Input type="password" autoComplete="new-password" {...form.register('password')} /></label>
          <label>Confirmation<Input type="password" autoComplete="new-password" {...form.register('confirmPassword')} /></label>
          {Object.values(form.formState.errors).map((error) => error?.message ? <p className="field-error" key={error.message}>{error.message}</p> : null)}
          {mutation.error ? <p className="error">{mutation.error instanceof ApiError ? mutation.error.message : 'Inscription impossible'}</p> : null}
          <Button type="submit" disabled={mutation.isPending}><UserPlus size={18} /> Creer mon compte</Button>
        </form>
        <Link to="/connexion">J'ai deja un compte</Link>
      </Card>
    </main>
  );
}
