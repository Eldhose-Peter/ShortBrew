export function LogoMark({ size = 22 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="0.5" y="0.5" width="23" height="23" rx="6" stroke="#F5B544" strokeOpacity="0.35" />
      {/* Steam rise */}
      <path
        d="M8 7.5 C 8 5.5, 9.5 5.5, 9.5 4 M12 7.5 C 12 5.5, 13.5 5.5, 13.5 4 M16 7.5 C 16 5.5, 17.5 5.5, 17.5 4"
        stroke="#F5B544"
        strokeWidth="1.3"
        strokeLinecap="round"
        className="animate-flow"
      />
      {/* Brew mug body */}
      <path
        d="M6.5 9.5h9v5.5a3.5 3.5 0 0 1-3.5 3.5h-2a3.5 3.5 0 0 1-3.5-3.5V9.5z"
        stroke="#F5B544"
        strokeWidth="1.6"
        strokeLinejoin="round"
        fill="none"
      />
      {/* Mug handle */}
      <path
        d="M15.5 11h1.8a2 2 0 0 1 0 4h-1.8"
        stroke="#F5B544"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      {/* Pulse dot */}
      <circle cx="13.5" cy="4" r="1.2" fill="#F5B544" className="animate-pulse_dot" />
    </svg>
  );
}

export function Logo({ size = 22 }: { size?: number }) {
  return (
    <div className="flex items-center gap-2">
      <LogoMark size={size} />
      <span className="font-display text-base font-semibold tracking-tight text-ink">
        Short<span className="text-signal">Brew</span>
      </span>
    </div>
  );
}
