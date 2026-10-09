// Sample data for the view catalogue (doc 02 section 7). Pure data.
export const notApplicable = ['error'];

/** @type {import('@panomc/plugin-kit').Samples} */
export default {
  filled: { session: 'user', props: { onRegister: () => {}, data: { status: { accountType: 'PREMIUM', premiumLinked: true, premiumUsername: 'Notch' }, microsoftLoginEnabled: true } } },
  empty: { session: 'user', props: { onRegister: () => {}, data: { status: { accountType: 'AUTO', premiumLinked: false }, microsoftLoginEnabled: true } } },
  loading: { session: 'user', props: { onRegister: () => {}, data: { status: null, microsoftLoginEnabled: false } } },
};
