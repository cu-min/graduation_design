type PagePlaceholderProps = {
  eyebrow: string;
  title: string;
  description: string;
};

function PagePlaceholder({ eyebrow, title, description }: PagePlaceholderProps) {
  return (
    <section className="page-card">
      <p className="page-eyebrow">{eyebrow}</p>
      <h1>{title}</h1>
      <p className="page-description">{description}</p>
    </section>
  );
}

export default PagePlaceholder;
