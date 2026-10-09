// Sample data for the view catalogue (doc 02 section 7). Pure data.
export const notApplicable = ['loading', 'error'];

/** @type {import('@panomc/plugin-kit').Samples} */
export default {
  filled: { props: { data: { status: { accountType: 'PREMIUM', premiumLinked: true, premiumUsername: 'Notch' } } } },
  empty: { props: { data: { status: null } } },
};
