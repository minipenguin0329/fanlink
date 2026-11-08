import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

export function ComposerChoices<T extends string | number>({ options, value, onChange }: {
  options: { label: string; value: T }[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <View style={composerStyles.choices}>
      {options.map(option => {
        const selected = value === option.value;
        return (
          <Pressable key={option.value} accessibilityRole="radio"
            accessibilityState={{ checked: selected }} onPress={() => onChange(option.value)}
            style={[composerStyles.choice, selected && composerStyles.selected]}>
            <Text style={[composerStyles.choiceText, composerStyles.label, selected && composerStyles.link]}>{option.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export const composerStyles = StyleSheet.create({
  form: { minWidth: 0, gap: 12 },
  panel: { minWidth: 0, gap: 9, padding: 14, borderRadius: 16, borderWidth: 1, borderColor: '#e1e9f5', backgroundColor: '#fff' },
  title: { fontSize: 25, fontWeight: '900', color: '#101623' },
  label: { fontSize: 13, fontWeight: '700', color: '#101623' },
  hint: { fontSize: 12, color: '#626b7a', lineHeight: 18 },
  link: { fontSize: 13, fontWeight: '700', color: '#075cf1' },
  choices: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  choice: { maxWidth: '100%', flexShrink: 1, flexDirection: 'row', alignItems: 'center', gap: 6, borderColor: '#d9e2ef', borderWidth: 1, borderRadius: 16, paddingHorizontal: 12, paddingVertical: 9, backgroundColor: '#fff' },
  selected: { borderColor: '#82b4ff', backgroundColor: '#e9f3ff' },
  choiceText: { flexShrink: 1, minWidth: 0 },
  input: { minHeight: 46, paddingHorizontal: 12, paddingVertical: 10, borderRadius: 13, borderWidth: 1, borderColor: '#d9e2ef', backgroundColor: '#f8faff', color: '#101623' },
  error: { color: '#ad2336', fontSize: 12, lineHeight: 18 },
  publish: { minHeight: 48, justifyContent: 'center', alignItems: 'center', padding: 12, backgroundColor: '#075cf1', borderRadius: 14 },
  publishText: { color: '#fff', fontWeight: '800', fontSize: 14, textAlign: 'center' },
});
