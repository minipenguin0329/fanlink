import createIconSet from '@expo/vector-icons/createIconSet';
import glyphMap from '@expo/vector-icons/build/vendor/react-native-vector-icons/glyphmaps/Ionicons.json';

// Keep the font in the app's assets so static hosts serve it with the web export.
export const Ionicons = createIconSet(glyphMap, 'ionicons', require('./assets/Ionicons.ttf'));
