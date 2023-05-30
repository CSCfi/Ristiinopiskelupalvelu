process.env.VUE_APP_VERSION = require('./package.json').version;

module.exports = {
  publicPath: '/admin-ui',
  outputDir: 'target/dist',
  devServer: {
    port: 3000,
    proxy: {
      '/admin-ui/api': {
        target: 'http://localhost:8081/admin-ui/api',
        changeOrigin: true,
        pathRewrite: {
          '^/admin-ui/api': ''
        }
      }
    }
  }
};
