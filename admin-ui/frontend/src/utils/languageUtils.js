export default {
  resolveText: function(currentLanguage, languages) {
    if (languages) {
      const textInCurrentLanguage = languages[currentLanguage];
      if (textInCurrentLanguage) {
        return textInCurrentLanguage;
      }

      for (const language of ['fi', 'sv', 'en']) {
        const text = languages[language];
        if (text) {
          return text;
        }
      }
    }

    return '(Undefined)';
  }
};
