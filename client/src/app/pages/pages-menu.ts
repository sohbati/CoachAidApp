import { NbMenuItem } from '@nebular/theme';

export const MENU_ITEMS: NbMenuItem[] = [
  {
    title: 'صفحه اصلی',
    icon: 'nb-home',
    link: '/pages/dashboard',
    home: true,
  },
  {
    title: 'اشخاص',
    icon: 'nb-person',
    link: '/pages/persons',
  },
  {
    title: 'حرکت ها',
    icon: 'nb-exercise',
    link: '/pages/exercises',
  },
  {
    title: 'برنامه ها',
    icon: 'nb-person',
    link: '/pages/programs',
  },
  {
    title: 'FEATURES',
    group: true,
  },
  {
    title: 'Auth',
    icon: 'nb-locked',
    children: [
      {
        title: 'Login',
        link: '/auth/login',
      },
      {
        title: 'Register',
        link: '/auth/register',
      },
      {
        title: 'Request Password',
        link: '/auth/request-password',
      },
      {
        title: 'Reset Password',
        link: '/auth/reset-password',
      },
    ],
  },
];
