import { forwardRef } from 'react';
import { Loader2 } from 'lucide-react';
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';

export function Button({ className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button className={`btn ${className}`.trim()} {...props} />;
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(function Input(props, ref) {
  return <input ref={ref} className="input" {...props} />;
});

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(function Textarea(props, ref) {
  return <textarea ref={ref} className="input textarea" {...props} />;
});

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(function Select(props, ref) {
  return <select ref={ref} className="input" {...props} />;
});

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`card ${className}`.trim()}>{children}</section>;
}

export function Badge({ children, tone = 'blue' }: { children: ReactNode; tone?: 'blue' | 'teal' | 'warning' | 'danger' }) {
  return <span className={`badge ${tone}`}>{children}</span>;
}

export function EmptyState({ title, text }: { title: string; text?: string }) {
  return <div className="empty"><strong>{title}</strong>{text ? <p>{text}</p> : null}</div>;
}

export function LoadingState() {
  return <p className="loading"><Loader2 size={16} className="spin" /> Chargement</p>;
}