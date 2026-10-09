// Sample data for the view catalogue (doc 02 section 7). Pure data.
// The page starts in its loading state and finishes the Microsoft round trip in the browser, so one state shows it.
export const notApplicable = ['empty', 'loading', 'error'];

/** @type {import('@panomc/plugin-kit').Samples} */
export default {
  filled: { props: { data: {} } },
};
