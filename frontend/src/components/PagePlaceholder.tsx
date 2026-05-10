type PagePlaceholderProps = {
  eyebrow?: string;
  title: string;
  description: string;
};

function PagePlaceholder({ eyebrow, title, description }: PagePlaceholderProps) {
  return (
    <section className="page-card">
      {eyebrow ? <p className="page-eyebrow">{eyebrow}</p> : null}
      <h1>{title}</h1>
      <p className="page-description">{description}</p>
    </section>
  );
}

export default PagePlaceholder;
