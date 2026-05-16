import type { SVGProps } from 'react';

type IconProps = SVGProps<SVGSVGElement> & {
  size?: string | number;
};

function IconBase({ size = 18, children, ...props }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      fill="none"
      aria-hidden="true"
      focusable="false"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      {children}
    </svg>
  );
}

export function ThumbsUpIcon(props: IconProps) {
  return (
    <IconBase {...props}>
      <path
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="4"
        d="M27.6 18.6v-7.2A5.4 5.4 0 0 0 22.2 6L15 22.2V42h20.916a3.6 3.6 0 0 0 3.6-3.06L42 22.74a3.6 3.6 0 0 0-3.6-4.14H27.6ZM15 22h-4.806C8.085 21.963 6.283 23.71 6 25.8v12.6a4.158 4.158 0 0 0 4.194 3.6H15V22Z"
      />
    </IconBase>
  );
}

export function StarIcon(props: IconProps) {
  return (
    <IconBase {...props}>
      <path
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="4"
        d="m23.999 5-6.113 12.478L4 19.49l10.059 9.834L11.654 43 24 36.42 36.345 43 33.96 29.325 44 19.491l-13.809-2.013L24 5Z"
      />
    </IconBase>
  );
}

export function DislikeIcon(props: IconProps) {
  return (
    <IconBase {...props}>
      <path
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="4"
        d="m24 31-3-5 7-6-9-5 1-5.8C18.5 8.432 16.8 8 15 8 8.925 8 4 12.925 4 19c0 11 13 21 20 23 7-2 20-12 20-23 0-6.075-4.925-11-11-11-1.8 0-3.5.433-5 1.2"
      />
    </IconBase>
  );
}

export function ShareOneIcon(props: IconProps) {
  return (
    <IconBase {...props}>
      <path
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="4"
        d="M35 16a5 5 0 1 0 0-10 5 5 0 0 0 0 10ZM13 29a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z"
      />
      <path
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="4"
        d="m30 13.575-12.66 7.67m-.002 5.319 13.34 7.883"
      />
      <path
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="4"
        d="M35 32a5 5 0 1 1 0 10 5 5 0 0 1 0-10Z"
      />
    </IconBase>
  );
}
