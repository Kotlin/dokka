/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');
const WebpackShellPluginNext = require('webpack-shell-plugin-next');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer');

module.exports = (env) => {
  const isMinify = env.minify === 'true';
  const isAnalyze = env.analyze === 'true';
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
      assetModuleFilename: '../images/[name][ext]',
    },
    module: {
      rules: [
        {
          test: /\.css$/,
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
              : null,
          ].filter(Boolean),
        },
        {
          test: /\.(woff|woff2|eot|ttf|otf)$/i,
          type: 'asset/resource',
          generator: {
            filename: 'fonts/[name][ext]'
          }
        },
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
                      plugins: [['cssnano']],
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
                plugins: [['cssnano']],
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
        onAfterDone: {
          scripts: [
            'echo "[ui-kit] Done rebuild, coping files to dokka-integration-tests"',
            'cp -r ../plugin-base/src/main/resources/dokka/ui-kit ../../dokka-integration-tests/gradle/build/ui-showcase-result',
          ],
          blocking: false,
          parallel: true,
        },
      }),
      ...(isAnalyze ? [new BundleAnalyzerPlugin()] : []),
    ],
  };
};
