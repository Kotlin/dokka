/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');

module.exports = {
    entry: {
        entry: './styles/index.scss',
    },
    mode: 'production',
    output: {
        path: path.resolve(__dirname, 'build'),
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
                                            preset: ['default', {discardComments: {removeAll: true}}],
                                        },
                                    ],
                                ],
                            },
                        },
                    },
                    'sass-loader',
                ],
            },
        ],
    },
    plugins: [
        new MiniCssExtractPlugin({
            filename: 'style.css',
        }),
    ],
};