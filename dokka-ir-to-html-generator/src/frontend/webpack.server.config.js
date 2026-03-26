const path = require('path');
const baseConfig = require('./webpack.config');

module.exports = {
  ...baseConfig,
  name: 'server',
  // IMPORTANT: Use 'web' or 'webworker', but NOT 'node'.
  // 'node' will add require('fs') and other modules that don't exist in GraalJS.
  target: 'web',
  entry: ['./src/server/polyfills.ts', './src/server/index.tsx'],  // SSR entry point (renderToString)
  output: {
    filename: 'server-bundle.js',
    // Put the result where Java will find it (as in your example)
    path: path.resolve(__dirname, '../main/resources'),

    // Critical for GraalJS:
    library: {
      name: 'SSR', // Variable name accessible from Java
      type: 'var', // Export via var SSR = ...
    },
    globalObject: 'this', // Prevents using window
    clean: false, // Do not clean if the folder is shared with other resources
  },
  optimization: {
    minimize: false, // For debugging in GraalJS, disable minification first
  },
  module: {
    rules: [
      ...baseConfig.module.rules,
      {
        test: /\.tsx?$/,
        use: [
          {
            loader: 'ts-loader',
            options: {
              configFile: 'tsconfig.json', // Use the server-side TS config
            },
          },
        ],
        exclude: /node_modules/,
      },
      // Handling styles on the server: we need to ignore CSS
      // but keep imports like import './style.css' valid
      {
        test: /\.(s[ac]ss|css)$/i,
        use: [
          {
            loader: 'css-loader',
            options: {
              // If you use CSS Modules, set exportOnlyLocals: true
              // For plain CSS — just let the loader pass, styles are not needed in JS
              modules: {
                mode: 'icss',
              },
            },
          },
          'sass-loader',
        ],
      },
    ],
  },
  // We do not use MiniCssExtractPlugin here because the server does not need .css files
  plugins: [],
};