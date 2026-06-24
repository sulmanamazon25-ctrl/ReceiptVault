-- Public bucket for legal/support static pages (works without GitHub Pages).
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'legal',
  'legal',
  true,
  1048576,
  array['text/html', 'text/plain']
)
on conflict (id) do update set public = true;

create policy "legal_public_read"
  on storage.objects for select
  to public
  using (bucket_id = 'legal');

create policy "legal_service_upload"
  on storage.objects for all
  to service_role
  using (bucket_id = 'legal')
  with check (bucket_id = 'legal');
