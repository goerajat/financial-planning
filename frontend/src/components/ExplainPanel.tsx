import React, { useEffect, useRef, useState } from 'react';
import { useExplain } from '../context/ExplainContext';
import { Explanation } from '../types';
import './ExplainPanel.css';

const MAX_QUESTION = 400;

const paragraphs = (text: string) => text.split(/\n\s*\n/).map(p => p.trim()).filter(Boolean);

const Answer: React.FC<{ text: string }> = ({ text }) => (
  <>{paragraphs(text).map((p, i) => <p key={i} className="explain-a">{p}</p>)}</>
);

const CloseIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
    <path d="M3.5 3.5l9 9M12.5 3.5l-9 9" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
  </svg>
);

/**
 * A side panel on a wide screen, the whole screen on a phone. Questions and
 * answers are the server's thread for the plan as it stands; a running
 * question shows its elapsed time so a half-minute wait reads as progress.
 */
const ExplainPanel: React.FC = () => {
  const { isOpen, close, thread, loading, error, notice, ask } = useExplain();
  const [draft, setDraft] = useState('');
  const [now, setNow] = useState(() => Date.now());
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const endRef = useRef<HTMLDivElement>(null);
  const running = thread?.running ?? false;
  const messages = thread?.messages ?? [];

  useEffect(() => {
    if (isOpen) inputRef.current?.focus();
  }, [isOpen]);

  useEffect(() => {
    if (!running) return;
    setNow(Date.now());
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, [running]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end' });
  }, [messages.length, running]);

  useEffect(() => {
    if (!isOpen) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, close]);

  if (!isOpen) return null;

  const send = (q: string) => {
    const t = q.trim();
    if (!t || running) return;
    setDraft('');
    ask(t);
  };

  const elapsed = (m: Explanation) => Math.max(0, Math.round((now - new Date(m.askedAt).getTime()) / 1000));
  const suggestions = (thread?.suggestions ?? []).filter(s => !messages.some(m => m.question === s));
  const earlier = thread?.earlier ?? [];

  return (
    <aside className="explain-panel" role="dialog" aria-label="Ask about this plan">
      <header className="explain-head">
        <h3>Ask about this plan</h3>
        <button type="button" className="explain-close" onClick={close} aria-label="Close">
          <CloseIcon />
        </button>
      </header>

      <div className="explain-body">
        {messages.length === 0 && (
          <p className="explain-intro">
            {loading
              ? 'Loading the conversation…'
              : 'Ask why a figure moves the way it does. The answer comes from the plan’s own rules and figures and takes about half a minute.'}
          </p>
        )}

        {messages.map(m => (
          <article key={m.id || m.askedAt} className={`explain-turn is-${m.status.toLowerCase()}`}>
            <p className="explain-q">{m.question}</p>
            {m.status === 'DONE' && m.answer && <Answer text={m.answer} />}
            {m.status === 'RUNNING' && (
              <p className="explain-wait" aria-live="polite">
                <span className="explain-spinner" aria-hidden="true" />
                Thinking… {elapsed(m)} s
              </p>
            )}
            {m.status === 'ERROR' && (
              <p className="explain-err">
                Could not answer: {m.error || 'unknown problem'}.
                <button type="button" className="explain-retry" onClick={() => send(m.question)} disabled={running}>
                  Try again
                </button>
              </p>
            )}
          </article>
        ))}

        {suggestions.length > 0 && !running && (
          <div className="explain-chips" aria-label="Suggested questions">
            {suggestions.map(s => (
              <button key={s} type="button" className="explain-chip" onClick={() => send(s)}>{s}</button>
            ))}
          </div>
        )}

        {earlier.length > 0 && (
          <details className="explain-earlier">
            <summary>Asked about earlier versions of the plan</summary>
            {earlier.map(m => (
              <article key={m.id} className="explain-turn is-done">
                <p className="explain-q">{m.question}</p>
                {m.answer && <Answer text={m.answer} />}
              </article>
            ))}
          </details>
        )}
        <div ref={endRef} />
      </div>

      {(error || notice) && (
        <p className={`explain-flash${error ? ' is-error' : ''}`} role="status">{error || notice}</p>
      )}

      <form className="explain-compose" onSubmit={e => { e.preventDefault(); send(draft); }}>
        <textarea
          ref={inputRef}
          value={draft}
          onChange={e => setDraft(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(draft); } }}
          rows={2}
          maxLength={MAX_QUESTION}
          placeholder="Why does Roth increase in 2036?"
          aria-label="Your question"
          disabled={running}
        />
        <button type="submit" disabled={running || !draft.trim()}>{running ? 'Thinking…' : 'Ask'}</button>
      </form>
    </aside>
  );
};

export default ExplainPanel;
