/**
 * Translation component that generates Handlebars syntax for translations
 * Usage: <Translation key="reset-password" fallback="Reset Password" />
 * Output: {{#if translation.reset-password}}{{translation.reset-password}}{{else}}Reset Password{{/if}}
 */
export const Translation = ({ key, fallback }) => {
  return `{{#if translation.${key}}}{{translation.${key}}}{{else}}${fallback}{{/if}}`;
};
