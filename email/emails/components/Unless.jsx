/**
 * Unless component that generates Handlebars syntax for negative conditional rendering
 */
export const Unless = ({ condition, children }) => {
  return (
    <div data-handlebars-unless={condition} style={{ display: 'contents' }}>
      {children}
    </div>
  );
};
