import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { UserPlus } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { me, registerPatient } from '../api/auth.api';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Logo } from '../components/Logo';
import { Button, Card, Input } from '../components/ui';

const schema = z.object({
  firstName: z.string().trim().min(1, 'Veuillez renseigner votre prenom.'),
  lastName: z.string().trim().min(1, 'Veuillez renseigner votre nom.'),
  email: z.string().trim().min(1, 'Veuillez renseigner votre adresse e-mail.').email('Veuillez saisir une adresse e-mail valide.'),
  password: z.string().min(8, 'Le mot de passe doit contenir au moins 8 caracteres.'),
  confirmPassword: z.string().min(1, 'Veuillez confirmer votre mot de passe.')
}).refine((value) => value.password === value.confirmPassword, { path: ['confirmPassword'], message: 'Les mots de passe ne correspondent pas.' });

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { setSession } = useAuth();
  const navigate = useNavigate();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const form = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { firstName: '', lastName: '', email: '', password: '', confirmPassword: '' } });
  const mutation = useMutation({
    mutationFn: async (payload: { firstName: string; lastName: string; email: string; password: string }) => {
      const response = await registerPatient(payload);
      const validatedUser = await me(response.accessToken);
      return { accessToken: response.accessToken, user: validatedUser };
    },
    onSuccess: ({ accessToken, user }) => {
      form.reset();
      setSuccessMessage('Compte cree avec succes. Redirection en cours...');
      setSession(accessToken, user);
      window.setTimeout(() => navigate('/tableau-de-bord', { replace: true }), 500);
    }
  });

  const submit = (values: FormValues) => {
    if (mutation.isPending) return;
    setSuccessMessage(null);
    mutation.mutate({
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      email: values.email.trim().toLowerCase(),
      password: values.password
    });
  };

  const registrationError = mutation.error instanceof ApiError
    ? registrationErrorMessage(mutation.error)
    : 'Inscription impossible pour le moment.';

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Logo />
        <h1>Creer mon passeport</h1>
        <p>Le role patient est attribue automatiquement.</p>
        <form noValidate onSubmit={form.handleSubmit(submit)}>
          <div className="two-cols">
            <label htmlFor="register-first-name">Prenom<Input id="register-first-name" autoComplete="given-name" aria-invalid={Boolean(form.formState.errors.firstName)} {...form.register('firstName')} /></label>
            <label htmlFor="register-last-name">Nom<Input id="register-last-name" autoComplete="family-name" aria-invalid={Boolean(form.formState.errors.lastName)} {...form.register('lastName')} /></label>
          </div>
          {form.formState.errors.firstName?.message ? <p className="field-error">{form.formState.errors.firstName.message}</p> : null}
          {form.formState.errors.lastName?.message ? <p className="field-error">{form.formState.errors.lastName.message}</p> : null}
          <label htmlFor="register-email">Email<Input id="register-email" type="email" autoComplete="email" aria-invalid={Boolean(form.formState.errors.email)} {...form.register('email')} /></label>
          {form.formState.errors.email?.message ? <p className="field-error">{form.formState.errors.email.message}</p> : null}
          <label htmlFor="register-password">Mot de passe<Input id="register-password" type="password" autoComplete="new-password" aria-invalid={Boolean(form.formState.errors.password)} {...form.register('password')} /></label>
          {form.formState.errors.password?.message ? <p className="field-error">{form.formState.errors.password.message}</p> : null}
          <label htmlFor="register-confirm-password">Confirmation<Input id="register-confirm-password" type="password" autoComplete="new-password" aria-invalid={Boolean(form.formState.errors.confirmPassword)} {...form.register('confirmPassword')} /></label>
          {form.formState.errors.confirmPassword?.message ? <p className="field-error">{form.formState.errors.confirmPassword.message}</p> : null}
          {successMessage ? <p className="success">{successMessage}</p> : null}
          {mutation.error ? <p className="error">{registrationError}</p> : null}
          <Button type="submit" disabled={mutation.isPending}><UserPlus size={18} /> Creer mon compte</Button>
        </form>
        <Link to="/connexion">J'ai deja un compte</Link>
      </Card>
    </main>
  );
}

function registrationErrorMessage(error: ApiError) {
  if (error.status === 409) return 'Cette adresse e-mail est deja utilisee.';
  if (error.status === 400) return 'Veuillez verifier les champs obligatoires.';
  return 'Inscription impossible pour le moment.';
}
