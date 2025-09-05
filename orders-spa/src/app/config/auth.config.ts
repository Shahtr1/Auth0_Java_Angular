export const authConfig = {
  domain: 'dev-adoz0mmq4w01wlnm.us.auth0.com',
  clientId: 'vLa9NPlK04AgVztAXCqABPP0GYjJNI7g',
  audience: 'https://orders-api',
  scope: [
    'openid',
    'profile',
    'email',
    'phone',
    'address',
    'offline_access',
    'read:orders',
    'write:orders',
  ].join(' '),
};
