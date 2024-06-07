/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');

module.exports = (env, args) => {
  const isMinify = env.minify === 'true';
  return {
    entry: {
      entry: ['./src/main/ui-kit/index.ts'],
    },
    mode: 'production',
    resolve: {
      extensions: ['.tsx', '.ts', '.js', '.svg'],
    },
    output: {
      path: path.resolve(__dirname, '../plugin-base/src/main/resources/dokka/ui-kit/'),
      filename: isMinify ? 'ui-kit.min.js' : 'ui-kit.js',
    },
    module: {
      rules: [
        {
          test: /\.scss$/,
          use: [
            MiniCssExtractPlugin.loader,
            'css-loader',
            isMinify
              ? {
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
                }
              : {
                  loader: 'sass-loader',
                  options: {
                    sassOptions: {
                      sourceMap: true,
                      minimize: false,
                      outputStyle: 'expanded',
                    },
                  },
                },
            'sass-loader',
          ],
        },
        {
          test: /\.tsx?$/,
          use: [
            {
              loader: 'ts-loader',
              options: {
                transpileOnly: true,
              },
            },
          ],
          exclude: /node_modules/,
        },
      ],
    },
    plugins: [
      new MiniCssExtractPlugin({
        filename: isMinify ? 'ui-kit.min.css' : 'ui-kit.css',
      }),
    ],
  };
};
