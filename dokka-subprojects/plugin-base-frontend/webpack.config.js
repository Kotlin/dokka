/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

const {join, resolve} = require('path');

const ringUiWebpackConfig = require('@jetbrains/ring-ui/webpack.config');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TerserPlugin = require("terser-webpack-plugin");

const pkgConfig = require('./package.json').config;

const componentsPath = join(__dirname, pkgConfig.components);

const webpackConfig = () => ({
  entry: `${componentsPath}/root.tsx`,
  resolve: {
    mainFields: ['module', 'browser', 'main'],
    extensions: ['.tsx', '.ts', '.js', '.svg'],
    alias: {
      react: resolve('./node_modules/react'),
      'react-dom': resolve('./node_modules/react-dom'),
      '@jetbrains/ring-ui': resolve('./node_modules/@jetbrains/ring-ui')
    }
  },
  output: {
    path: resolve(__dirname, pkgConfig.dist),
    filename: '[name].js',
    publicPath: '',
    devtoolModuleFilenameTemplate: '/[absolute-resource-path]'
  },
  module: {
    rules: [
      ...ringUiWebpackConfig.config.module.rules,
      {
        test: /\.s[ac]ss$/i,
        use: [
          MiniCssExtractPlugin.loader,
          'css-loader',
          'sass-loader',
        ],
        include: componentsPath,
        exclude: ringUiWebpackConfig.componentsPath,
      },
      {
        test: /\.tsx?$/,
        use: [
          {
            loader: 'ts-loader',
            options: {
              transpileOnly: true
            }
          }
        ]
      },
      {
        test: /\.svg$/,
        loader: require.resolve('svg-inline-loader'),
        options: {removeSVGTagAttrs: false},
        include: [require('@jetbrains/icons')]
      }
    ]
  },
  plugins: [
    new MiniCssExtractPlugin({
      // Options similar to the same options in webpackOptions.output
      // both options are optional
      filename: '[name].css',
      chunkFilename: '[id].css',
    }),
  ],
  optimization: {
      minimize: true,
      minimizer: [new TerserPlugin({
        extractComments: false,
      })],
  },
  output: {
    path: __dirname + '/dist/'
  }
});

module.exports = webpackConfig;
