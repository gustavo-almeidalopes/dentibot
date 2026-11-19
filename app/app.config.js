// Versão de exibição: uma fonte só, o package.json (`npm version patch|minor|major`).
// Os números de build (versionCode/buildNumber) são do EAS — appVersionSource: remote.
module.exports = ({ config }) => ({ ...config, version: require('./package.json').version });
