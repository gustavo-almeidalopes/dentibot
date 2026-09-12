/* As duas URLs de auth num lugar só: o ClerkProvider as usa para redirecionar
   e o widget para montar o link "criar conta", que sem isso apontava de volta
   para o sign-in. O ?criar=1 é o que a tela lê para decidir qual widget
   mostrar — assim o link do Clerk e o botão da página levam ao mesmo lugar. */
export const LOGIN = '/login';
export const CRIAR = '/login?criar=1';
