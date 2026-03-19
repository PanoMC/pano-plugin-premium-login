import {render} from '@react-email/render';
import * as fs from 'fs';
import * as path from 'path';
import {glob} from 'glob';
import React from 'react';

async function buildHandlebars() {
  const outputDir = path.join(process.cwd(), '..', 'src', 'main', 'resources', 'mail');
  
  // Output dizinini oluştur
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  // Email dosyalarını bul
  const emailFiles = await glob('emails/**/*.jsx', {
    ignore: ['**/static/**', '**/*.test.jsx', '**/*.spec.jsx', '**/components/**'],
  });

  console.log(`Found ${emailFiles.length} email templates`);

  for (const emailFile of emailFiles) {
    try {
      // Email modülünü import et
      const emailModule = await import(
        path.resolve(process.cwd(), emailFile)
      );
      
      const EmailComponent = emailModule.default;
      
      if (!EmailComponent) {
        console.warn(`No default export found in ${emailFile}, skipping...`);
        continue;
      }

      // Preview props'ları al (varsa)
      const previewProps = EmailComponent.PreviewProps || {};
      
      // Tüm props'larla render et
      const element = React.createElement(EmailComponent, previewProps);
      const html = await render(element);

      // Handlebars HTML'i başlat
      let handlebarsHtml = html;
      
      // Translation placeholder'larını önce çevir
      let previousHtml = '';
      let iterations = 0;
      while (previousHtml !== handlebarsHtml && iterations < 10) {
        previousHtml = handlebarsHtml;
        iterations++;
        handlebarsHtml = handlebarsHtml.replace(
          /\{\{TRANSLATION:([^:]+):([^}]+)\}\}/g,
          (match, key, fallback) => {
            let processedFallback = fallback;
            if (processedFallback.includes('{{TRANSLATION:')) {
              processedFallback = processedFallback.replace(
                /\{\{TRANSLATION:([^:]+):([^}]+)\}\}/g,
                '{{#if translation.$1}}{{translation.$1}}{{else}}$2{{/if}}'
              );
            }
            return `{{#if translation.${key}}}{{translation.${key}}}{{else}}${processedFallback}{{/if}}`;
          }
        );
      }
      
      // Eksik {{/if}} kapanışlarını düzelt
      handlebarsHtml = handlebarsHtml.replace(
        /\{\{TRANSLATION:([^:]+):([^}]+)\}(?!\}\})/g,
        (match, key, fallback) => {
          let processedFallback = fallback;
          if (processedFallback.includes('{{TRANSLATION:')) {
            processedFallback = processedFallback.replace(
              /\{\{TRANSLATION:([^:]+):([^}]+)\}\}/g,
              '{{#if translation.$1}}{{translation.$1}}{{else}}$2{{/if}}'
            );
          }
          return `{{#if translation.${key}}}{{translation.${key}}}{{else}}${processedFallback}{{/if}}`;
        }
      );

      // If ve Unless component'lerini Handlebars syntax'ına çevir
      handlebarsHtml = handlebarsHtml.replace(
        /<div[^>]*data-handlebars-if="([^"]+)"[^>]*data-handlebars-else="true"[^>]*>([\s\S]*?)<div[^>]*data-handlebars-else-content[^>]*>([\s\S]*?)<\/div><\/div>/g,
        (match, condition, content, elseContent) => {
          const cleanContent = content.replace(/<div[^>]*data-handlebars-[^>]*>[\s\S]*?<\/div>/g, '').trim();
          const cleanElseContent = elseContent.replace(/<div[^>]*data-handlebars-[^>]*>[\s\S]*?<\/div>/g, '').trim();
          return `{{#if ${condition}}}${cleanContent}{{else}}${cleanElseContent}{{/if}}`;
        }
      );

      handlebarsHtml = handlebarsHtml.replace(
        /<div[^>]*data-handlebars-if="([^"]+)"[^>]*(?!data-handlebars-else)[^>]*>([\s\S]*?)<\/div>/g,
        (match, condition, content) => {
          const cleanContent = content.replace(/<div[^>]*data-handlebars-[^>]*>[\s\S]*?<\/div>/g, '').trim();
          return `{{#if ${condition}}}${cleanContent}{{/if}}`;
        }
      );

      handlebarsHtml = handlebarsHtml.replace(
        /<div[^>]*data-handlebars-unless="([^"]+)"[^>]*>([\s\S]*?)<\/div>/g,
        (match, condition, content) => {
          const cleanContent = content.replace(/<div[^>]*data-handlebars-[^>]*>[\s\S]*?<\/div>/g, '').trim();
          return `{{#unless ${condition}}}${cleanContent}{{/unless}}`;
        }
      );
      
      // Preview props'larındaki değerleri Handlebars syntax'ına çevir
      const sortedKeys = Object.keys(previewProps).sort((a, b) => {
        const aVal = String(previewProps[a] ?? '');
        const bVal = String(previewProps[b] ?? '');
        return bVal.length - aVal.length;
      });

      sortedKeys.forEach((key) => {
        const value = previewProps[key];
        if (value != null && value !== '') {
          const stringValue = String(value);
          handlebarsHtml = handlebarsHtml.replace(
            new RegExp(`="${escapeRegex(stringValue)}"`, 'g'),
            `="{{${key}}}"`
          );
          handlebarsHtml = handlebarsHtml.replace(
            new RegExp(`href="${escapeRegex(stringValue)}"`, 'g'),
            `href="{{${key}}}"`
          );
          handlebarsHtml = handlebarsHtml.replace(
            new RegExp(`>${escapeRegex(stringValue)}<`, 'g'),
            `>{{${key}}}<`
          );
          handlebarsHtml = handlebarsHtml.replace(
            new RegExp(`>\\s*${escapeRegex(stringValue)}\\s*<`, 'g'),
            `>{{${key}}}<`
          );
        }
      });

      // Dosya adını oluştur
      const fileName = path.basename(emailFile, '.jsx');
      const outputPath = path.join(outputDir, `${fileName}.hbs`);

      // Handlebars dosyasını kaydet
      fs.writeFileSync(outputPath, handlebarsHtml, 'utf-8');
      console.log(`✓ Built ${fileName}.hbs`);
    } catch (error) {
      console.error(`Error building ${emailFile}:`, error);
      if (error instanceof Error) {
        console.error(error.stack);
      }
    }
  }

  console.log(`\n✓ Build complete! Handlebars templates saved to ${outputDir}`);
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

buildHandlebars().catch(console.error);
