/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');
const WebpackShellPluginNext = require('webpack-shell-plugin-next');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');

module.exports = (env, args) => {
  const isMinify = env.minify === 'true';
  return {
    watch: env.watch === 'true',
    devServer: {
      hot: true,
      port: env.port || 8001,
      open: true,
      static: {
        directory: '../../dokka-integration-tests/gradle/build/ui-showcase-result',
      },
      devMiddleware: {
        writeToDisk: (filePath) => {
          return !(/\.hot-update\.(js|json)$/.test(filePath) || /\.LICENSE\.txt$/.test(filePath));
        },
      },
      watchFiles: {
        paths: ['../../dokka-integration-tests/gradle/build/ui-showcase-result'],
        options: {
          ignored: ['../../dokka-integration-tests/gradle/build/ui-showcase-result/ui-kit']
        }
      }
    },
    entry: {
      entry: ['./src/main/ui-kit/index.ts'],
    },
    mode: 'production',
    resolve: {
      extensions: ['.tsx', '.ts', '.js', '.svg'],
    },
    output: {
      path: path.resolve(__dirname, '../plugin-base/src/main/resources/dokka/ui-kit/'),
      filename: 'ui-kit.min.js',
      assetModuleFilename: 'assets/[name][ext]',
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
        {
          test: /\.(png|jpe?g|gif|svg)$/i,
          type: 'asset/resource',
        },
      ],
    },
    optimization: {
      minimize: isMinify,
      minimizer: [
        '...', // Extends existing minimizers
        new CssMinimizerPlugin({
          minimizerOptions: {
            preset: [
              'default',
              {
                discardComments: { removeAll: true },
                normalizeWhitespace: isMinify,
                plugins: [
                  [
                    'cssnano',
                  ],
                ]
              },
            ],
          },
        }),
      ],
    },
    plugins: [
      new MiniCssExtractPlugin({
        filename: isMinify ? 'ui-kit.min.css' : 'ui-kit.css',
      }),
      new WebpackShellPluginNext({
        onDoneWatch: {
          scripts: [
            'echo "Done rebuild, coping files to dokka-integration-tests"',
            'cp -r ../plugin-base/src/main/resources/dokka/ui-kit ../../dokka-integration-tests/gradle/build/ui-showcase-result',
          ],
          blocking: false,
          parallel: true,
        },
      }),
    ],
  };
};
