const path = require('path');

const isProduction = process.env.NODE_ENV === 'production';

// Shared configuration for client and server
module.exports = {
  mode: isProduction ? 'production' : 'development',
  devtool: isProduction ? 'source-map' : 'eval-source-map',
  resolve: {
    extensions: ['.tsx', '.ts', '.js', '.jsx', '.json'],
    alias: {
      // Aliases, if any (e.g., @components -> src/components)
    },
  },
  module: {
    rules: [
      // Rules for images and fonts
      {
        test: /\.(png|jpg|jpeg|gif|svg|woff|woff2|ttf|eot)$/,
        type: 'asset/resource',
      },
    ],
  },
};