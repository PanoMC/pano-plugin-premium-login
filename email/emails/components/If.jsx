/**
 * If component that generates Handlebars syntax for conditional rendering
 */
export const If = ({ condition, children, else: elseContent }) => {
  return (
    <div data-handlebars-if={condition} data-handlebars-else={elseContent ? 'true' : undefined} style={{ display: 'contents' }}>
      {children}
      {elseContent && (
        <div data-handlebars-else-content style={{ display: 'contents' }}>
          {elseContent}
        </div>
      )}
    </div>
  );
};
