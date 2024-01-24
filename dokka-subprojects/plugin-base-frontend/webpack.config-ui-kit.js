/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');

module.exports = {
  entry: {
    entry: ['./src/main/ui-kit/index.js'],
  },
  mode: 'production',
  output: {
    path: path.resolve(__dirname, '../plugin-base/src/main/resources/dokka/ui-kit/'),
    filename: 'ui-kit.js',
  },
  module: {
    rules: [
      {
        test: /\.scss$/,
        use: [
          MiniCssExtractPlugin.loader,
          'css-loader',
          {
            loader: 'postcss-loader',
            options: {
              postcssOptions: {
                plugins: [
                  [
                    'cssnano',
                    {
                      preset: ['default', { discardComments: { removeAll: true } }],
                    },
                  ],
                ],
              },
            },
          },
          'sass-loader',
        ],
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env']
          }
        }
      }
    ],
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: 'ui-kit.css',
    }),
  ],
};
